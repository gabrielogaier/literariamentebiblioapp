# Estado técnico

## Objetivo

Biblioteca Android pessoal, offline, com foco em quem está com cada livro. Sem servidor ou minilinux.

## Arquitetura

Java 17, Activity e views nativas. SQLiteOpenHelper versão 1. Android mínimo 23 e compile/target 35. AGP 8.9.2 e Gradle 8.11.1. Sem dependências externas no código de execução. Executor para HTTP e arquivos de backup. Seletor de documentos Android para exportação/restauração. Capas JPEG em Base64 no banco e no backup.

## Arquivos relevantes

- MainActivity.java: telas, navegação, empréstimos, formulários e IO de backup.
- Db.java: esquema, restrições, persistência e restauração transacional.
- BookSearch.java: pesquisa, detalhes de uma edição e download opcional de capa.
- README.md: geração do APK e uso.
- validacao/test_schema.py: quatro verificações SQLite executadas.
- validacao/TESTE_ANDROID.md: verificação manual pendente.

## Regras de negócio

Título e autor são os únicos campos obrigatórios. Cada cadastro representa um exemplar. Só há um empréstimo ativo por exemplar. Pessoas podem ser criadas na hora do empréstimo. Devolução atualiza a data e preserva o registro. Sem prazo obrigatório. Backup substitui toda a base somente após confirmação; versão, tipos, IDs, referências, datas e duplicidade de empréstimos ativos são validados antes da alteração. Erro dentro da transação causa rollback.

## Funcionalidades implementadas

Dashboard; cadastro/edição; busca local; empréstimos; devolução individual; pessoas e totais; histórico geral/por livro/por pessoa; consulta opcional e preenchimento assistido; capas locais; backup/restauração; preservação de rascunho durante recriação da Activity.

## Validação e problemas conhecidos

Fontes compilados separadamente sem erros com API 35. Quatro testes SQLite passaram. Build Android, lint, inspeção visual e testes em aparelho não realizados por ausência de SDK e falha do download de Gradle. APIs tradicionais de Activity/ProgressDialog/insets geram avisos de depreciação. Migração acima da versão 1 ainda não existe; qualquer nova versão do esquema precisa de migração explícita.

## Limites e riscos

Consulta depende da disponibilidade e cobertura da Open Library. Resultado pode ter edição diferente; usuário deve revisar. Backup JSON sem criptografia, limite de 50 MB; sem cópia automática ou sincronização. Listas são renderizadas integralmente e a exportação mantém JSON em memória, adequadas para biblioteca pessoal pequena. Não há exclusão de livros/pessoas na primeira versão. Nome é único apenas por convenção de interface, permitindo distinguir homônimos por nome identificável. Histórico usa os nomes e títulos atuais, preservando IDs/datas de empréstimos.

## Próximos passos

1. Abrir no Android Studio, sincronizar e executar assembleDebug/lintDebug.
2. Validar todos os fluxos de validacao/TESTE_ANDROID.md no aparelho.
3. Gerar APK assinado com chave própria para atualizações estáveis.
4. Antes de mudar esquema ou assinatura, exportar backup e definir migração compatível.

## Publicação e CI

Projeto preparado para repositório público. codemagic.yaml contém workflow android-debug com Java 17 e saída APK debug. Não há senhas, chaves de assinatura ou dados pessoais reais nos arquivos versionados. .gitignore bloqueia bancos e backups locais, arquivos .env e artefatos Android. Build remoto permanece pendente.
