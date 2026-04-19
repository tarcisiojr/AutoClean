## ADDED Requirements

### Requirement: Identificar consumo de cache por app
O sistema SHALL listar apps com seus tamanhos de cache, ordenados por consumo decrescente, usando StorageStatsManager ou `dumpsys diskstats`.

#### Scenario: Listagem de cache
- **WHEN** o ciclo de limpeza ou o dashboard solicita informacoes de cache
- **THEN** o sistema retorna lista de apps com: nome, pacote, tamanho de cache em MB, ultima limpeza

### Requirement: Limpeza de cache de app especifico
O sistema SHALL limpar o cache de um app especifico via `pm clear --cache-only <pacote>` ou fallback para `rm -rf /data/data/<pacote>/cache/*` via Shizuku.

#### Scenario: Limpeza de cache com sucesso
- **WHEN** o sistema executa limpeza de cache de um app
- **THEN** o cache do app e removido e o sistema registra: pacote, tamanho liberado, timestamp

#### Scenario: Limpeza de cache sem Shizuku ativo
- **WHEN** o Shizuku nao esta ativo
- **THEN** o sistema MUST pular a limpeza e registrar no log que a acao foi adiada por falta de privilegio

### Requirement: Limpeza automatica por threshold
O sistema SHALL executar limpeza automatica de cache quando o cache total de apps ultrapassar um threshold configuravel (padrao: 1GB).

#### Scenario: Cache total acima do threshold
- **WHEN** a soma de cache de todos os apps > 1GB (configuravel)
- **THEN** o sistema limpa cache dos apps com maior consumo, comecando pelo maior, ate que o total fique abaixo do threshold

### Requirement: Lista de apps de streaming prioritarios
O sistema SHALL manter uma lista de apps de streaming cujo cache e priorizado para limpeza: com.netflix.ninja, com.amazon.amazonvideo.livingroom, com.wbd.stream, com.disney.disneyplus, com.apple.atve.androidtv.appletv, br.com.skymais, com.globo.globotv, com.spotify.tv.android, br.com.claro.now.smarttvclient.

#### Scenario: Limpeza prioriza streaming
- **WHEN** a limpeza automatica e acionada
- **THEN** o sistema limpa primeiro o cache dos apps da lista de streaming antes de outros apps
