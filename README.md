# IGNpcUtils

IGNpcUtils é um plugin que foi feito de uma necessidade para meu servidor [AfterLands](https://afterlands.com/) e que agora estou disponibilizando para a comunidade. 

O plugin fornece utilitários para gerenciar NPCs (um addon) utilizando APIs do plugin [**Citizens2**](https://www.spigotmc.org/resources/citizens.13811/). Ele permite ocultar, exibir e gerenciar NPCs (por jogador ou globalmente) de forma eficiente, muito útil para inúmeros fins.

## 📋 Features

- **Gerenciamento de NPCs**:
  - Ocultar e exibir NPCs para jogadores específicos.
  - Gerenciar NPCs ocultos e visíveis por padrão (globalmente).
  - Mover NPCs para mundos e coordenadas específicas.
 
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
| `/npcutils move <id> <world> <x> <y> <z> <pitch> <yaw>`      | Mover um NPC para coordenadas específicas.        |

## 🛠️ Dependências

- **Minecraft**: Versão 1.8.8
- [**Citizens2**](https://www.spigotmc.org/resources/citizens.13811/) (v2.0.35 ou superior).

## 🧑‍💻 Desenvolvedor

- **Autor**: IceGames
- **GitHub**: [IceGames23](https://github.com/IceGames23)
- **Discord:** icegames

## 📄 Licença

Este projeto está licenciado sob a licença MIT. Consulte o arquivo `LICENSE` para mais detalhes.
