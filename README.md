# IGNpcUtils

IGNpcUtils é um plugin para servidores Minecraft que fornece utilitários para gerenciar NPCs utilizando o plugin **Citizens2**. Ele permite ocultar, exibir e gerenciar NPCs de forma eficiente, além de salvar configurações no banco de dados.

## 📋 Funcionalidades

- **Gerenciamento de NPCs**:
  - Ocultar e exibir NPCs para jogadores específicos.
  - Gerenciar NPCs ocultos e visíveis por padrão.
- **Integração com Citizens2**:
  - Dependência obrigatória para o funcionamento do plugin.
- **Persistência de Dados**:
  - Suporte a banco de dados configurável (MySQL, SQLite, etc.).
- **Configurações Personalizáveis**:
  - Arquivos de configuração para mensagens e ajustes gerais.

## 📜 Comandos

| Comando                     | Descrição                                   | Permissão         |
|-----------------------------|---------------------------------------------|-------------------|
| `/npcutils show <id> <player>` | Exibe um NPC para um jogador específico.   | `npcutils.admin`  |
| `/npcutils hide <id> <player>` | Oculta um NPC de um jogador específico.    | `npcutils.admin`  |
| `/npcutils showall <id>`      | Exibe um NPC para todos os jogadores.       | `npcutils.admin`  |
| `/npcutils hideall <id>`      | Oculta um NPC de todos os jogadores.        | `npcutils.admin`  |

## 🛠️ Dependências

- **Minecraft**: Versão 1.8.8
- [**Citizens2**](https://www.spigotmc.org/resources/citizens.13811/): Plugin obrigatório (v2.0.35 ou superior).

## 🚀 Instalação

1. Baixe o arquivo `.jar` do plugin.
2. Coloque o arquivo na pasta `plugins` do seu servidor Minecraft.
3. Certifique-se de que o plugin **Citizens2** está instalado (2.0.35 ou superior).
4. Inicie o servidor para gerar os arquivos de configuração.
5. Configure o arquivo `config.yml` e, se necessário, o `messages.yml`.

## 🧑‍💻 Desenvolvedor

- **Autor**: IceGames
- **GitHub**: [IceGames23](https://github.com/IceGames23)

## 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.
