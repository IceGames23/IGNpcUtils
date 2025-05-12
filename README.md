# IGNpcUtils

IGNpcUtils é um plugin para servidores Minecraft que fornece utilitários para gerenciar NPCs utilizando o plugin **Citizens2**. Ele permite ocultar, exibir e gerenciar NPCs (por jogador ou globalmente) de forma eficiente.

## 📋 Funcionalidades

- **Gerenciamento de NPCs**:
  - Ocultar e exibir NPCs para jogadores específicos.
  - Gerenciar NPCs ocultos e visíveis por padrão.
- **MySQL & SQLite**:
  - Suporte a banco de dados configurável (MySQL, SQLite, etc.) para armazenamento de dados dos jogadores.
- **Configurações Personalizáveis**:
  - Arquivos de configuração para mensagens totalmente personalizáveis.

## 📜 Comandos & Permissões

| Permissão                     | Descrição                                   |
|-----------------------------|---------------------------------------------|
| `npcutils.admin` | Acesso a todos os comandos admin do plugin.   |
| `npcutils.bypass` | Habilidade de ver todos os NPCs globalmente ocultos.    |

| Comando                     | Descrição                                   |
|-----------------------------|---------------------------------------------|
| `/npcutils show <id> <player>` | Exibe um NPC para um jogador específico.   |
| `/npcutils hide <id> <player>` | Oculta um NPC de um jogador específico.    |
| `/npcutils showall <id>`      | Exibe um NPC para todos os jogadores.       |
| `/npcutils hideall <id>`      | Oculta um NPC de todos os jogadores.        |

## 🛠️ Dependências

- **Minecraft**: Versão 1.8.8
- [**Citizens2**](https://www.spigotmc.org/resources/citizens.13811/) (v2.0.35 ou superior).

## 🧑‍💻 Desenvolvedor

- **Autor**: IceGames
- **GitHub**: [IceGames23](https://github.com/IceGames23)

## 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.
