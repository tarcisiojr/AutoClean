## ADDED Requirements

### Requirement: Listar bloatware identificado
O sistema SHALL apresentar uma lista de apps de sistema identificados como bloatware, com nome, pacote, consumo de RAM atual e status (ativo/congelado).

#### Scenario: Exibicao da lista de bloatware
- **WHEN** o usuario acessa a secao de bloatware no dashboard
- **THEN** o sistema exibe todos os apps com prefixo com.tcl.* que nao estao na whitelist imutavel, mostrando nome, pacote, RSS e status

### Requirement: Congelar app de sistema
O sistema SHALL permitir congelar (desabilitar) um app de sistema via `pm disable-user --user 0 <pacote>` executado via Shizuku.

#### Scenario: Congelamento de app seguro
- **WHEN** o usuario solicita congelar um app que nao esta na whitelist imutavel
- **THEN** o sistema executa `pm disable-user --user 0 <pacote>` via Shizuku e atualiza o status para "congelado"

#### Scenario: Tentativa de congelar app protegido
- **WHEN** o usuario tenta congelar um app da whitelist imutavel (ex: com.tcl.tvinput)
- **THEN** o sistema MUST recusar a operacao e exibir mensagem explicando que o app e essencial

### Requirement: Descongelar app de sistema
O sistema SHALL permitir descongelar (reabilitar) um app previamente congelado via `pm enable <pacote>` executado via Shizuku.

#### Scenario: Descongelamento de app
- **WHEN** o usuario solicita descongelar um app congelado
- **THEN** o sistema executa `pm enable <pacote>` via Shizuku e atualiza o status para "ativo"

### Requirement: Lista curada de bloatware seguro
O sistema SHALL incluir uma lista pre-configurada de apps TCL seguros para congelar, baseada em diagnostico real: com.tcl.gamebar, com.tcl.eva, com.tcl.smartalexa, com.tcl.esticker, com.tcl.hotelmenu, com.tcl.ocean.instructions, com.tcl.channelplus, com.tcl.waterfall.overseas, com.tcl.gallery, com.tcl.hearaid.

#### Scenario: App na lista curada
- **WHEN** um app esta na lista curada de bloatware seguro
- **THEN** o sistema exibe um indicador visual de "seguro para congelar" no dashboard

### Requirement: Congelamento em lote
O sistema SHALL permitir congelar todos os apps da lista curada de uma vez com uma unica acao.

#### Scenario: Congelamento em lote
- **WHEN** o usuario seleciona "Congelar todos os recomendados"
- **THEN** o sistema congela todos os apps da lista curada que ainda estao ativos, exibindo progresso
