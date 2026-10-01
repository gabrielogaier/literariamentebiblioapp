# Registro de execução

## 2026-10-01

Objetivo: criar projeto Android local de biblioteca pessoal para gerar APK, conforme requisitos fornecidos.

Alterações: aplicação Java nativa, tema e ícone; SQLite com books/people/loans; cadastro e edição; dashboard; pessoas; empréstimos e devolução; histórico; pesquisa local; consulta opcional HTTPS Open Library; capa offline; backup JSON e restauração validada/transacional; Gradle Wrapper; documentação e roteiro de validação.

Arquivos: settings.gradle, build.gradle, gradle.properties, gradlew, gradlew.bat, gradle/wrapper/*, app/build.gradle, AndroidManifest.xml, Db.java, MainActivity.java, BookSearch.java, styles.xml, ic_book.xml, README.md, validacao/*, execucao.md, handoff.md.

Validação: fontes Java compilados com Eclipse ECJ 3.40.0/JDK 17 contra API Android 35. Houve inicialmente conflito entre classes Java do android.jar e módulos do JDK; a verificação foi repetida com jar de API sem esses pacotes duplicados. Compilação sem erros; avisos de APIs Android antigas, mantidas para compatibilidade com minSdk 23. Quatro testes de restrições SQLite passaram: cadastro mínimo, bloqueio de empréstimo duplicado, histórico após devolução/novo empréstimo, rejeição de pessoa inexistente/data inválida. XML, Wrapper e ZIP inspecionados.

Limitação: sem SDK Android, build APK e lint não executados. O Wrapper foi invocado, mas o download de Gradle falhou com UnknownHostException para services.gradle.org neste ambiente. Sem emulador/aparelho, não houve teste visual, teste Android da restauração nem consulta HTTP real à Open Library.

Situação: projeto fonte entregue em ZIP. Geração do APK e validação em aparelho pendentes, com instruções no README e roteiro próprio.

## 2026-10-01: preparação para repositório público

Revisados fontes, documentação e arquivos para publicação. Não encontrados dados pessoais reais ou credenciais. Nomes usados em testes são fictícios. Ampliado .gitignore para bloquear configurações locais, credenciais de assinatura, bancos, backups e binários gerados. Incluído codemagic.yaml com workflow de APK debug, sem chaves ou envio externo de mensagens. Publicação inicial do projeto autorizada pelo responsável no repositório público informado. Build no Codemagic ainda não executado.
