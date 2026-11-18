package diet

import (
	"slices"
	"strings"
	"testing"
)

func TestFoodSearchTokens(t *testing.T) {
	cases := []struct {
		name string
		in   string
		want []string
	}{
		{"acento e caixa", "Açúcar", []string{"acucar"}},
		{"frase com pontuacao", "  FILÉ  de frango ", []string{"file", "de", "frango"}},
		{"abreviacoes descartadas", "frango, c/ sal", []string{"frango", "sal"}},
		{"palavra repetida", "Arroz arroz ARROZ", []string{"arroz"}},
		{"somente uma letra", "a", nil},
		{"vazio", "   ", nil},
	}

	for _, test := range cases {
		t.Run(test.name, func(t *testing.T) {
			got := foodSearchTokens(test.in)
			if len(test.want) == 0 && len(got) == 0 {
				return
			}
			if !slices.Equal(got, test.want) {
				t.Fatalf("foodSearchTokens(%q) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestFoodSearchTokensLimitsWords(t *testing.T) {
	got := foodSearchTokens("um dois tres quatro cinco seis sete oito nove dez onze")
	if len(got) != foodSearchMaxTokens {
		t.Fatalf("esperava %d tokens, veio %d", foodSearchMaxTokens, len(got))
	}
}

func TestTranslateMappingHasSameLength(t *testing.T) {
	if len([]rune(accentedRunes)) != len([]rune(plainRunes)) {
		t.Fatalf("mapeamento de acentos desalinhado: %d vs %d", len([]rune(accentedRunes)), len([]rune(plainRunes)))
	}
}

func TestFoodNameNormalizedCoversEveryAccentedRune(t *testing.T) {
	accented := []rune(accentedRunes)
	plain := []rune(plainRunes)
	for i, r := range accented {
		lower := accentFolder.Replace(strings.ToLower(string(r)))
		expected := strings.ToLower(string(plain[i]))
		if lower != expected {
			t.Fatalf("runa %q mapeia para %q, esperado %q", r, lower, expected)
		}
	}
}
