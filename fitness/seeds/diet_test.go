package seeds

import (
	"bytes"
	"encoding/csv"
	"strconv"
	"strings"
	"testing"
)

func TestComplementaryFoods(t *testing.T) {
	foods := loadComplementaryFoods()
	if len(foods) < 30 {
		t.Fatalf("esperado ao menos 30 itens no complemento, veio %d", len(foods))
	}

	seen := make(map[string]bool, len(foods))
	var isoCalories, isoProtein float64
	for _, food := range foods {
		if food.Code == "" || food.Name == "" {
			t.Errorf("item sem code ou nome: %+v", food)
		}
		if len(food.Code) > 20 {
			t.Errorf("code %q excede 20 caracteres", food.Code)
		}
		if seen[food.Code] {
			t.Errorf("code duplicado no complemento: %q", food.Code)
		}
		seen[food.Code] = true
		// Códigos do complemento não podem colidir com os números da TACO.
		if _, err := strconv.Atoi(food.Code); err == nil {
			t.Errorf("code %q deveria ter prefixo SUP-/IND-", food.Code)
		}
		if food.Protein < 0 || food.Carbs < 0 || food.Fat < 0 || food.Fiber < 0 {
			t.Errorf("macros negativos em %q", food.Code)
		}
		if food.Code == "SUP-WHEY-ISO" {
			isoCalories, isoProtein = food.Calories, food.Protein
		}
	}

	if isoCalories != 370 || isoProtein != 85 {
		t.Errorf("SUP-WHEY-ISO inesperado: %.1f kcal, %.1f g de proteína", isoCalories, isoProtein)
	}
}

func TestPortionsMatchCatalog(t *testing.T) {
	foods := loadFoodFromExcel()
	foods = append(foods, loadComplementaryFoods()...)
	if len(foods) < 600 {
		t.Fatalf("catálogo inesperadamente pequeno: %d itens", len(foods))
	}

	codes := make(map[string]bool, len(foods))
	for _, food := range foods {
		codes[food.Code] = true
	}

	contents, err := Files.ReadFile(foodPortionFile)
	if err != nil {
		t.Fatal(err)
	}
	records, err := csv.NewReader(bytes.NewReader(contents)).ReadAll()
	if err != nil {
		t.Fatal(err)
	}

	seen := make(map[string]bool, len(records))
	count := 0
	for index, record := range records[1:] {
		if len(record) < 3 {
			t.Errorf("linha %d da porção com colunas insuficientes", index+2)
			continue
		}
		code := strings.TrimSpace(record[0])
		label := strings.TrimSpace(record[1])
		grams := parseFloat(record[2])
		if code == "" || label == "" {
			t.Errorf("linha %d da porção sem code ou label", index+2)
		}
		if !codes[code] {
			t.Errorf("porção para code inexistente no catálogo: %q", code)
		}
		if seen[code] {
			t.Errorf("code duplicado nas porções: %q", code)
		}
		seen[code] = true
		if grams <= 0 {
			t.Errorf("porção de %q com gramas inválidas: %v", code, grams)
		}
		count++
	}

	if count < 60 {
		t.Errorf("esperado ao menos 60 porções, veio %d", count)
	}

	applyFoodPortions(foods, loadFoodPortions())
	for _, food := range foods {
		if food.Code != "488" {
			continue
		}
		if food.PortionLabel != "unidade" || food.PortionGrams != 50 {
			t.Errorf("ovo cozido (488) com porção inesperada: %q %.1f g", food.PortionLabel, food.PortionGrams)
		}
	}
}
