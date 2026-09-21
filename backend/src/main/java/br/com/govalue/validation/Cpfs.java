package br.com.govalue.validation;

/** Utilitarios de CPF: normalizacao e validacao pelos digitos verificadores (RNF003 do TCC). */
public final class Cpfs {

    private Cpfs() {}

    /** Remove pontuacao, deixando apenas os digitos. */
    public static String normalizar(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public static boolean valido(String cpf) {
        String d = normalizar(cpf);
        if (d == null || d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (d.charAt(i) - '0') * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }
}
