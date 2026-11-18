package seeds

import (
	"bytes"
	"health/schemas/diet"
	"log"
	"strconv"
	"strings"

	"github.com/xuri/excelize/v2"
	"gorm.io/gorm"
)

const (
	foodFile       = "data/taco.xlsx"
	foodSheet      = "CMVCol taco3"
	foodFattySheet = "AGtaco3"
	foodHeaderRows = 3
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

func loadFoodFromExcel(db *gorm.DB) {
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
		return
	}

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

		if err := db.Create(&food).Error; err != nil {
			log.Printf("Erro ao inserir %s: %v", food.Name, err)
		}
	}
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
