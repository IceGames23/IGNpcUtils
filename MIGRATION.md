# Config Migration Guide 📦

## Automatic Migration

O plugin **migra automaticamente** configs antigas para o novo formato ao iniciar.

### Formato Antigo (v1.x)
```yaml
defaultHidden:
  - 160
  - 161
  - 162
  - 163
```

### Formato Novo (v2.0+)
```yaml
npcs:
  160:
    states:
      default:
        visible: false
        actions: []
  161:
    states:
      default:
        visible: false
        actions: []
```

## O Que Acontece na Migração

1. ✅ **Backup automático** criado em `config.yml.backup`
2. ✅ **Conversão automática** de todos NPCs em `defaultHidden`
3. ✅ **Criação de estados** com `visible: false`
4. ✅ **Remoção** da seção `defaultHidden`
5. ✅ **Log completo** no console

## Log Example

```
[IGNpcUtils] =========================================
[IGNpcUtils] Config migration started (v1.x -> v2.0)
[IGNpcUtils] =========================================
[IGNpcUtils] Found 8 NPCs in defaultHidden
[IGNpcUtils] Config backup created: config.yml.backup
[IGNpcUtils]   - Migrated NPC 160 (default hidden)
[IGNpcUtils]   - Migrated NPC 161 (default hidden)
[IGNpcUtils]   - Migrated NPC 162 (default hidden)
[IGNpcUtils]   - Migrated NPC 163 (default hidden)
[IGNpcUtils]   - Migrated NPC 148 (default hidden)
[IGNpcUtils]   - Migrated NPC 165 (default hidden)
[IGNpcUtils]   - Migrated NPC 158 (default hidden)
[IGNpcUtils]   - Migrated NPC 206 (default hidden)
[IGNpcUtils] =========================================
[IGNpcUtils] Migration complete!
[IGNpcUtils]   - 8 NPCs migrated
[IGNpcUtils]   - Backup saved: config.yml.backup
[IGNpcUtils] =========================================
```

## Rollback (Se Necessário)

Se algo der errado:
```bash
cd plugins/IGNpcUtils
cp config.yml.backup config.yml
```

## Notas

- ⚠️ Migração roda **uma única vez** (detecta `defaultHidden`)
- ✅ NPCs já migrados são **ignorados**
- ✅ Totalmente **seguro** - cria backup antes
- ✅ **Idempotente** - pode rodar múltiplas vezes sem problemas
