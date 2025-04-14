# Arquitetura

Exemplo de arquitetura.

## Passos para executar o projeto

1. Instale os pré-requisitos;
2. Clone o projeto;
3. Crie a configuração;
4. Instale os pacotes NPM;
5. Execute o projeto.

## Instale os pré-requisitos

Instale:

- `Git`: Última versão.
- `Docker`: Última versão.
- `Visual Studio Code`: Última versão e instale as extensões recomendadas após clonar o projeto.

## Clone o projeto

Clone o projeto usando HTTPS ou SSH, preferencialmente SSH.

## Crie a configuração

Crie uma cópia do arquivo `.sample-env`, renomeie para `.env` e siga as instruções no arquivo.

## Construa as imagens Docker

Execute o comando na pasta raiz:

```
docker-compose build
```

## Instale os pacotes NPM

Execute os comandos na pasta raiz:

```
docker-compose run --rm --no-deps server npm install
```

## Execute o projeto

Execute o comando na pasta raiz:

```
docker-compose up -d
```
