# Arquitetura

Exemplo de arquitetura DDD em Java Quarkus.

## Passos para executar o projeto

1. Instale os pré-requisitos;
2. Clone o projeto;
3. Crie a configuração;
4. Execute o projeto.

## Instale os pré-requisitos

Instale:

- `Git`: Última versão.
- `Docker`: Última versão.
- `JDK 25`: Eclipse Temurin 25 para compilações locais.
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

## Execute o projeto

Execute o comando na pasta raiz:

```
docker-compose up -d
```

## Execute os testes

Requer JDK 25, Docker (o `mvn verify` sobe um MySQL 8.4 via Dev Services) e acesso à rede no primeiro build.

```bash
mvn test      # testes unitários (lógica pura)
mvn verify    # unitários + integração; relatório de cobertura em target/jacoco-report/index.html
```

## Contrato da API

- `POST` responde 201 com `Location`; `PUT` e `PATCH` respondem 200; `DELETE` responde 204.
- Listagens retornam `{ "result": [...], "total": n }` com `offset >= 0` e `limit` de 0 a 100.
- Filtros por query no formato `criterio|valor` (sem `|` vale `eq`): `eq`, `ne`, `gt`, `ge`, `lt`, `le`,
  `startsWith`, `endsWith`, `contains` (e as negações), `between`/`notBetween` (dois valores) e `in`/`notIn`.
  Datas não aceitam fuso/offset (`2026-01-31` ou `2026-01-31T10:00:00`); campos inteiros só aceitam inteiros.
- Ordenação `campo|ASC` ou `campo|DESC`, vários campos separados por vírgula; a prioridade segue a ordem da query
  e o `id` é sempre o desempate final.
- Itens da fatura são retornados em ordem de `id`.
- Lote de itens (`items` no `PUT`/`PATCH` da fatura) é aplicado como delta, na ordem update → remove → create.
  Um `id` não pode aparecer duas vezes em `update` nem em `update` e `remove` ao mesmo tempo. O `id` e o
  `version` enviados no corpo dos itens nunca alteram o item. O `PUT` da fatura não substitui os itens, só aplica o delta.

### Concorrência (ETag / If-Match)

Clientes, produtos e faturas devolvem o cabeçalho `ETag` (a versão do registro) em `POST`, `PUT`, `PATCH` e `GET /{id}`.
Os endpoints de itens (`/invoices/{id}/items...`) usam o `ETag` da **fatura**: qualquer alteração de item muda a versão dela.

`PUT`, `PATCH` e `DELETE` (e `POST /invoices/{id}/items`) **exigem** `If-Match`:

| Situação | Resposta |
|---|---|
| sem `If-Match` | 428 |
| `If-Match` diferente da versão atual | 412 |
| `If-Match: *` | aceito, sem comparar versão |
| recurso inexistente | 404 (vem antes da checagem do cabeçalho) |

Mesmo com `If-Match: *`, duas gravações simultâneas sobre a mesma versão são detectadas e uma delas recebe 409.

### Erros

Todos os erros têm o formato `{ "message": "..." }`; erros de validação acrescentam `"violations": [{ "path", "message" }]`.

| Status | Quando |
|---|---|
| 400 | corpo vazio ou malformado, validação, regra de negócio, filtro/ordenação inválidos, valor fora da faixa |
| 404 | recurso (ou item) inexistente no caminho ou no lote de itens |
| 409 | unicidade violada em corrida, registro em uso (FK), registro alterado por outra requisição |
| 412 | `If-Match` não confere com a versão atual |
| 428 | `If-Match` ausente em `PUT`, `PATCH` ou `DELETE` |
| 422 | cliente ou produto referenciado no corpo não existe |
| 500 | erro inesperado, com mensagem genérica |

Nome de cliente/produto e número de fatura são únicos (regra no código e constraint no banco).
A fatura não repete produto: a regra fica no código, e o incremento forçado de versão ao gravar faz com que
duas requisições simultâneas na mesma fatura não consigam gravar ambas.
