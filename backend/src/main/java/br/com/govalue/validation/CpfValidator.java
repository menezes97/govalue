package br.com.govalue.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext context) {
        return valor == null || valor.isBlank() || Cpfs.valido(valor);
    }
}
