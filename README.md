# Literariamente BíblioApp

Aplicativo Android nativo para biblioteca pessoal, com foco em saber quem está com cada livro.

## O que está implementado

- Dashboard com total, emprestados, disponíveis e pessoas com livros.
- Cadastro e edição com apenas título e autor obrigatórios.
- Busca local por título, autor e ISBN.
- Empréstimo para pessoa existente ou pessoa cadastrada na hora.
- Devolução individual, sem apagar o histórico.
- Pessoas com quantidade de livros e histórico individual.
- Histórico geral e por livro.
- Consulta opcional por título, autor ou ISBN à Open Library.
- Seleção de resultado e preenchimento dos campos encontrados.
- Capa armazenada no próprio banco, disponível offline e incluída no backup.
- Exportação e restauração de backup JSON pelo seletor de arquivos do Android.
- Confirmação antes de substituir a biblioteca e restauração em transação.
- Interface em português com navegação Início, Livros e Emprestar.

## Gerar o APK no Windows com Android Studio

1. Extraia o ZIP para uma pasta normal, por exemplo `C:\Projetos\literariamente-biblioapp`.
2. Abra o Android Studio e escolha **Open**, selecionando a pasta que contém `settings.gradle`.
3. Aguarde o Gradle Sync. A primeira compilação precisa de internet no computador para baixar as ferramentas.
4. Se solicitado, instale **Android SDK Platform 35** e **Build Tools 35.0.0** pelo SDK Manager.
5. Use o JDK 17 ou o JDK compatível integrado ao Android Studio nas configurações do Gradle. O projeto usa Gradle 8.11.1 e Android Gradle Plugin 8.9.2.
6. Abra o terminal do Android Studio e execute:

```powershell
.\gradlew.bat assembleDebug
```

O arquivo será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Copie esse arquivo para o celular, abra e autorize a instalação pela origem utilizada, se o Android solicitar. O app exige Android 6.0 ou superior.

Em Linux ou macOS:

```bash
chmod +x gradlew
./gradlew assembleDebug
```

O Gradle Wrapper está incluído no ZIP. Não é necessário instalar Gradle separado. O Android Studio configura `local.properties` com o caminho do SDK. Se usar o terminal fora dele, configure o SDK com `ANDROID_HOME` ou crie esse arquivo localmente.

### Para uso duradouro

O APK debug serve para teste e uso pessoal. Para atualizações estáveis, gere um APK assinado no Android Studio em **Build > Generate Signed Bundle / APK > APK**. Crie sua própria chave e guarde-a em segurança. Use sempre o mesmo identificador de aplicativo e a mesma chave nas futuras versões, aumentando `versionCode` para atualizar sem desinstalar. Uma chave debug de outro computador pode não ser compatível com a instalação existente. Exporte backup antes de substituir ou desinstalar.

## Uso

1. Toque em **Livros > Cadastrar livro**.
2. Informe título e autor. Os demais campos podem ficar vazios.
3. Opcionalmente, toque em **Buscar informações online**, escolha um resultado, confirme, revise e salve.
4. Toque em **Emprestar**, selecione um livro disponível e uma pessoa ou cadastre um nome.
5. No início, em **Com quem estão**, abra a pessoa e toque em **Devolver** no livro correto.

Cada cadastro representa um exemplar físico. Se tiver duas cópias iguais, cadastre duas vezes. Os seletores exibem o identificador do exemplar para distingui-los.

## Backup e restauração

No início, use **Exportar backup**. O Android permite escolher um local no aparelho ou um provedor como Drive/OneDrive quando instalado e disponível no seletor. Não há integração própria com contas online.

O backup inclui livros, capas, pessoas e empréstimos ativos e devolvidos. Ele contém os nomes das pessoas e não é criptografado. Guarde-o em um local particular e mantenha cópia fora do aparelho.

Para restaurar, escolha **Restaurar backup**, selecione o JSON e confirme. A restauração substitui toda a biblioteca; não faz mesclagem. Arquivos inválidos não devem alterar o banco. O limite de arquivo é 50 MB. A exportação automática pelo Android está desativada; este aplicativo usa o backup manual explícito.

## Consulta externa

A consulta só ocorre quando solicitada. Não envia pessoas nem histórico. Título, autor ou ISBN digitados são enviados à Open Library por HTTPS. Não usa login ou chave de API.

A busca retorna até 15 resultados. Os detalhes vêm de uma edição do resultado selecionado; confira ISBN, editora e ano porque pode ser outra edição ou idioma. Alguns dados podem faltar. A descrição pode vir da obra. Se a capa ou a descrição falharem, as demais informações ainda podem ser utilizadas. Um erro na consulta nunca impede o cadastro manual.

Documentação: https://openlibrary.org/dev/docs/api/search e https://openlibrary.org/developers/api

## Estrutura

```text
app/src/main/java/br/com/literariamente/biblioapp/
  MainActivity.java   Telas, navegação, formulários e seletor de backup
  Db.java             SQLite, empréstimos e backup transacional
  BookSearch.java     Consulta HTTPS opcional e cache de capa
app/src/main/res/     Tema e ícone
validacao/           Verificação das restrições SQLite
execucao.md          Registro de execução
handoff.md           Estado técnico e próximos passos
```

Os dados ficam no armazenamento privado do aplicativo. Não há servidor, minilinux, WebView, PWA, login ou sincronização.

## Estado da validação

Os fontes Java foram compilados separadamente contra a API Android 35, usando compilador Eclipse ECJ e JDK 17. As restrições do esquema SQLite foram exercitadas no computador. XML e integridade do ZIP foram verificados.

O APK completo não foi gerado neste ambiente: não há SDK Android instalado e o download do Gradle pelo Wrapper falhou na resolução de `services.gradle.org`. A compilação separada dos fontes não substitui o build Android, os testes em aparelho nem a inspeção visual. Execute `assembleDebug`, `lintDebug` e o roteiro de `validacao/TESTE_ANDROID.md` antes de considerar a versão validada para uso real.

## Gerar no Codemagic

Conecte este repositório ao Codemagic e selecione o workflow `android-debug` definido em `codemagic.yaml`. Ao concluir o build, baixe o APK em Artifacts. A configuração gera APK debug e não precisa de chaves próprias. Não configura publicação em loja nem envio de e-mail. Para uma versão release assinada, use o armazenamento de credenciais do serviço; nunca salve keystore ou senhas no repositório.

Referência: https://docs.codemagic.io/yaml-quick-start/building-a-native-android-app/
