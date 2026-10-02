package br.com.govalue.web.dto;

/** Resultado de POST /api/auth/login: ou o login ja terminou (LoginResponse, caminho de hoje,
 * payload inalterado), ou falta o segundo fator facial (LoginDesafioFacialResponse). Jackson
 * serializa pelo tipo concreto em tempo de execucao, entao quem nunca ativou o 2FA nunca ve
 * diferenca nenhuma na resposta. */
public sealed interface LoginResultado permits AuthDtos.LoginResponse, FaceDtos.LoginDesafioFacialResponse {}
