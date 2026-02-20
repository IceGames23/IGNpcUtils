# Changelog

## [2.2.1] - 2026-02-19

### Adicionado
- **CRUD de Skins Completo**:
  - Comando `list` para ver todas as skins salvas.
  - Comando `remove` para deletar skins salvas.
  - Comando `update` para atualizar skins existentes via URL.
  - Melhorias nas mensagens de feedback.

## [2.2.0] - 2026-02-19

### Adicionado
- **Sistema de Gerenciamento de Skins**:
  - Novo comando `npcutils skin save <name> <url>` para salvar templates de skin.
  - Novo comando `npcutils skin set <template/URL> [target]` para aplicar skins salvas ou via URL.
  - Suporte para aplicar skins em grupos de NPCs.
  - Arquivo `skins.yml` para armazenamento de templates.

### Alterado
- Refatoração interna do comando `skin` para suportar novas funcionalidades.

## [2.1.0]

### Adicionado
- Melhorias gerais e correções de bugs menores.

## [2.0.1]

### Corrigido
- Adicionado timeout nas requisições de skin (Mineskin API) para evitar travamento do servidor.
- Notificação de erro ao usuário quando a API de skin falha.

## [2.0.0]

### Adicionado
- **Novo Sistema de Configuração**: Migração automática do formato antigo para o novo.
- **Estados de NPC**: Introdução de estados visuais e ações configuráveis.
- **Grupos**: Capacidade de agrupar NPCs para ações em massa.
- **Aliases**: Apelidos para NPCs facilitarem o uso de comandos.
