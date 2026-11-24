package config

import (
	"os"
	"reflect"
	"regexp"
	"testing"

	"gorm.io/gorm/schema"
)

var (
	coveredPattern = regexp.MustCompile(`(?s)v_covered\s+(?:text\[\]\s*)?:?=\s*ARRAY\s*\[(.*?)\]`)
	quotedPattern  = regexp.MustCompile(`'([^']+)'`)
)

func TestLgpdPurgeCoversModels(t *testing.T) {
	content, err := os.ReadFile("lgpd_purge.sql")
	if err != nil {
		t.Fatalf("ler lgpd_purge.sql: %v", err)
	}
	covered := quotedGroup(coveredPattern, string(content))
	if len(covered) == 0 {
		t.Fatal("v_covered não encontrado em lgpd_purge.sql")
	}

	catalogs := map[string]bool{"exercises": true, "foods": true}
	naming := schema.NamingStrategy{}
	modelTables := map[string]bool{}

	for _, model := range domainModels {
		typ := reflect.TypeOf(model).Elem()
		table := naming.TableName(typ.Name())
		modelTables[table] = true
		if catalogs[table] {
			continue
		}
		if !covered[table] {
			t.Errorf("tabela %s (model %s) não está coberta por lgpd_purge_user; atualize lgpd_purge.sql", table, typ.Name())
		}
	}

	for table := range covered {
		if !modelTables[table] {
			t.Errorf("tabela coberta %s não corresponde a nenhum model GORM; renomeou ou removeu?", table)
		}
	}
	// Catálogos entram em v_covered só para o drift check ignorá-los; nunca podem ser apagados.
	for table := range catalogs {
		if !covered[table] {
			t.Errorf("catálogo global %s deve constar em v_covered (ignorado pelo drift check)", table)
		}
		deletePattern := regexp.MustCompile(`(?i)delete\s+from\s+` + table + `\b`)
		if deletePattern.MatchString(string(content)) {
			t.Errorf("lgpd_purge.sql não pode apagar o catálogo global %s", table)
		}
	}
}

func quotedGroup(pattern *regexp.Regexp, sql string) map[string]bool {
	values := map[string]bool{}
	match := pattern.FindStringSubmatch(sql)
	if match == nil {
		return values
	}
	for _, quoted := range quotedPattern.FindAllStringSubmatch(match[1], -1) {
		values[quoted[1]] = true
	}
	return values
}
