package seeds

import (
	"bytes"
	"encoding/csv"
	"health/schemas/diet"
	"log"
	"strconv"
	"strings"

	"github.com/xuri/excelize/v2"
	"gorm.io/gorm"
	"gorm.io/gorm/clause"
)

const (
	foodFile              = "data/taco.xlsx"
	foodComplementaryFile = "data/complementary.csv"
	foodPortionFile       = "data/portions.csv"
	foodSheet             = "CMVCol taco3"
	foodFattySheet        = "AGtaco3"
	foodHeaderRows        = 3
	foodSeedBatch         = 100
)

// Colunas da aba de composição (0-based), conforme o layout da TACO.
const (
	colNumber      = 0
	colName        = 1
	colWater       = 2
	colCalories    = 3
	colEnergyKJ    = 4
	colProtein     = 5
	colFat         = 6
	colCholesterol = 7
	colCarbs       = 8
	colFiber       = 9
	colAsh         = 10
	colCalcium     = 11
	colMagnesium   = 12
	colPhosphor    = 15
	colIron        = 16
	colSodium      = 17
	colPotassium   = 18
	colCopper      = 19
	colZinc        = 20
	colVitaminA    = 23
	colB1          = 24
	colB2          = 25
	colB6          = 26
	colNiacin      = 27
	colVitaminC    = 28
)

// Colunas da aba de ácidos graxos (0-based).
const (
	fattySaturated = 2
	fattyMono      = 3
	fattyPoly      = 4
	fattyTrans18   = 23
	fattyTrans182  = 24
)

type fattyAcids struct {
	saturated float64
	mono      float64
	poly      float64
	trans     float64
}

func loadFoodFromExcel() []diet.Food {
	contents, err := Files.ReadFile(foodFile)
	if err != nil {
		log.Fatal(err)
	}

	f, err := excelize.OpenReader(bytes.NewReader(contents))
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		_ = f.Close()
	}()

	fatty := loadFattyAcids(f)
	rows, err := f.GetRows(foodSheet, excelize.Options{RawCellValue: true})
	if err != nil {
		log.Fatal(err)
	}
	if len(rows) <= foodHeaderRows {
		log.Printf("planilha de alimentos vazia")
		return nil
	}

	foods := make([]diet.Food, 0, len(rows)-foodHeaderRows)
	for _, row := range rows[foodHeaderRows:] {
		number, ok := foodNumber(row)
		if !ok {
			continue
		}
		name := strings.TrimSpace(cell(row, colName))
		if name == "" {
			continue
		}

		acids := fatty[number]
		food := diet.Food{
			Code:         strconv.Itoa(number),
			Name:         name,
			Water:        parseFloat(cell(row, colWater)),
			Calories:     parseFloat(cell(row, colCalories)),
			EnergyKJ:     parseFloat(cell(row, colEnergyKJ)),
			Protein:      parseFloat(cell(row, colProtein)),
			Fat:          parseFloat(cell(row, colFat)),
			Cholesterol:  parseFloat(cell(row, colCholesterol)),
			Carbs:        parseFloat(cell(row, colCarbs)),
			Fiber:        parseFloat(cell(row, colFiber)),
			Ash:          parseFloat(cell(row, colAsh)),
			Calcium:      parseFloat(cell(row, colCalcium)),
			Magnesium:    parseFloat(cell(row, colMagnesium)),
			Phosphor:     parseFloat(cell(row, colPhosphor)),
			Iron:         parseFloat(cell(row, colIron)),
			Sodium:       parseFloat(cell(row, colSodium)),
			Potassium:    parseFloat(cell(row, colPotassium)),
			Copper:       parseFloat(cell(row, colCopper)),
			Zinc:         parseFloat(cell(row, colZinc)),
			VitaminA:     parseFloat(cell(row, colVitaminA)),
			B1:           parseFloat(cell(row, colB1)),
			B2:           parseFloat(cell(row, colB2)),
			B6:           parseFloat(cell(row, colB6)),
			Niacin:       parseFloat(cell(row, colNiacin)),
			VitaminC:     parseFloat(cell(row, colVitaminC)),
			SaturatedFat: acids.saturated,
			MonounsatFat: acids.mono,
			PolyunsatFat: acids.poly,
			TransFat:     acids.trans,
		}

		foods = append(foods, food)
	}

	return foods
}

// loadComplementaryFoods lê os itens que a TACO não cobre (suplementos e
// industrializados comuns). Os valores são médias de rótulo por 100 g, não
// medidas da TACO; o cabeçalho do CSV é code,name,calories,protein,carbs,fat,fiber.
func loadComplementaryFoods() []diet.Food {
	contents, err := Files.ReadFile(foodComplementaryFile)
	if err != nil {
		log.Fatal(err)
	}

	reader := csv.NewReader(bytes.NewReader(contents))
	reader.FieldsPerRecord = -1
	records, err := reader.ReadAll()
	if err != nil {
		log.Fatal(err)
	}

	foods := make([]diet.Food, 0, len(records))
	for index, record := range records {
		if index == 0 {
			continue
		}
		if len(record) < 7 {
			log.Printf("complemento de alimentos: linha %d ignorada (colunas insuficientes)", index+1)
			continue
		}
		code := strings.TrimSpace(strings.TrimPrefix(record[0], "\ufeff"))
		name := strings.TrimSpace(record[1])
		if code == "" || name == "" {
			continue
		}

		foods = append(foods, diet.Food{
			Code:     code,
			Name:     name,
			Calories: parseFloat(record[2]),
			Protein:  parseFloat(record[3]),
			Carbs:    parseFloat(record[4]),
			Fat:      parseFloat(record[5]),
			Fiber:    parseFloat(record[6]),
		})
	}

	return foods
}

type foodPortion struct {
	label string
	grams float64
}

// loadFoodPortions lê as medidas caseiras de referência (code,label,grams),
// por exemplo ovo inteiro = 1 unidade de 50 g. A quantidade da refeição segue
// em gramas; a porção é só um atalho de preenchimento.
func loadFoodPortions() map[string]foodPortion {
	contents, err := Files.ReadFile(foodPortionFile)
	if err != nil {
		log.Fatal(err)
	}

	reader := csv.NewReader(bytes.NewReader(contents))
	reader.FieldsPerRecord = -1
	records, err := reader.ReadAll()
	if err != nil {
		log.Fatal(err)
	}

	portions := make(map[string]foodPortion, len(records))
	for index, record := range records {
		if index == 0 {
			continue
		}
		if len(record) < 3 {
			log.Printf("porções: linha %d ignorada (colunas insuficientes)", index+1)
			continue
		}
		code := strings.TrimSpace(strings.TrimPrefix(record[0], "\ufeff"))
		label := strings.TrimSpace(record[1])
		grams := parseFloat(record[2])
		if code == "" || label == "" || grams <= 0 {
			continue
		}
		if _, exists := portions[code]; exists {
			log.Printf("porções: code duplicado ignorado: %s", code)
			continue
		}
		portions[code] = foodPortion{label: label, grams: grams}
	}

	return portions
}

func applyFoodPortions(foods []diet.Food, portions map[string]foodPortion) {
	found := make(map[string]bool, len(portions))
	for i := range foods {
		portion, ok := portions[foods[i].Code]
		if !ok {
			continue
		}
		foods[i].PortionLabel = portion.label
		foods[i].PortionGrams = portion.grams
		found[foods[i].Code] = true
	}

	for code := range portions {
		if !found[code] {
			log.Printf("porção sem alimento correspondente no catálogo: %s", code)
		}
	}
}

// seedFoods grava o catálogo com upsert por code: itens novos entram e os
// existentes são atualizados a cada boot, sem exigir truncate em produção.
// Alimentos nunca são editados pelo cliente, então sobrescrever é seguro; o
// id é preservado (meal_foods continua referenciando a mesma linha).
func seedFoods(db *gorm.DB, foods []diet.Food) {
	if len(foods) == 0 {
		return
	}

	unique := make([]diet.Food, 0, len(foods))
	seen := make(map[string]struct{}, len(foods))
	for _, food := range foods {
		if _, ok := seen[food.Code]; ok {
			log.Printf("alimento duplicado no seed ignorado: %s", food.Code)
			continue
		}
		seen[food.Code] = struct{}{}
		unique = append(unique, food)
	}

	err := db.Clauses(clause.OnConflict{
		Columns:   []clause.Column{{Name: "code"}},
		DoUpdates: clause.AssignmentColumns(foodSeedColumns),
	}).CreateInBatches(unique, foodSeedBatch).Error
	if err != nil {
		log.Printf("Erro ao gravar catálogo de alimentos: %v", err)
	}
}

// foodSeedColumns mantém o catálogo sincronizado no upsert; deleted_at volta a
// NULL para ressuscitar um item apagado.
var foodSeedColumns = []string{
	"name",
	"portion_label",
	"portion_grams",
	"calories",
	"protein",
	"carbs",
	"fat",
	"fiber",
	"water",
	"sodium",
	"cholesterol",
	"saturated_fat",
	"monounsat_fat",
	"polyunsat_fat",
	"trans_fat",
	"calcium",
	"iron",
	"magnesium",
	"phosphor",
	"potassium",
	"zinc",
	"vitamin_a",
	"vitamin_c",
	"vitamin_d",
	"vitamin_e",
	"b1",
	"b2",
	"b6",
	"b12",
	"available_carbs",
	"ash",
	"alcohol",
	"added_sugar",
	"added_salt",
	"selenium",
	"copper",
	"niacin",
	"folate",
	"energy_kj",
	"deleted_at",
	"updated_at",
}

// loadFattyAcids indexa a aba de ácidos graxos pelo número do alimento; a TACO
// só traz a composição lipídica de parte dos itens.
func loadFattyAcids(f *excelize.File) map[int]fattyAcids {
	acids := make(map[int]fattyAcids)
	rows, err := f.GetRows(foodFattySheet, excelize.Options{RawCellValue: true})
	if err != nil {
		log.Printf("Erro ao ler ácidos graxos: %v", err)
		return acids
	}
	for _, row := range rows[foodHeaderRows:] {
		number, ok := foodNumber(row)
		if !ok {
			continue
		}
		acids[number] = fattyAcids{
			saturated: parseFloat(cell(row, fattySaturated)),
			mono:      parseFloat(cell(row, fattyMono)),
			poly:      parseFloat(cell(row, fattyPoly)),
			trans:     parseFloat(cell(row, fattyTrans18)) + parseFloat(cell(row, fattyTrans182)),
		}
	}
	return acids
}

func foodNumber(row []string) (int, bool) {
	value := parseFloat(cell(row, colNumber))
	number := int(value)
	if number <= 0 || float64(number) != value {
		return 0, false
	}
	return number, true
}

func cell(row []string, index int) string {
	if index < 0 || index >= len(row) {
		return ""
	}
	return row[index]
}

func parseFloat(v string) float64 {
	v = strings.TrimSpace(v)
	v = strings.ReplaceAll(v, ",", ".")

	if v == "" {
		return 0
	}

	f, err := strconv.ParseFloat(v, 64)
	if err != nil {
		return 0
	}

	return f
}
