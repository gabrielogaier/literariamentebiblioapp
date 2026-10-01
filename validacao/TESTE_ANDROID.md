# Validação no Android

Usar emulador ou aparelho de teste sem dados reais. Build e lint:

```powershell
.\gradlew.bat assembleDebug lintDebug
```

- Ativar modo avião, iniciar o aplicativo e cadastrar livro apenas com título e autor.
- Tentar salvar sem título ou autor e verificar os erros no formulário.
- Editar um livro, cancelar alterações e confirmar que os dados anteriores continuam.
- Girar o aparelho durante o cadastro e conferir a preservação do rascunho, incluindo capa.
- Cadastrar dois exemplares do mesmo título e verificar os identificadores no seletor.
- Emprestar para uma nova pessoa; emprestar outro livro para a mesma pessoa.
- Conferir contadores do dashboard e os dois livros na pessoa.
- Devolver um livro e conferir que o outro continua ativo e o histórico é preservado.
- Confirmar que um livro emprestado não aparece no seletor de disponíveis.
- Devolver e emprestar o mesmo livro novamente, preservando os dois registros.
- Reiniciar o aplicativo e o aparelho, ainda offline, e conferir dados e capas.
- Buscar online por título/autor e por ISBN, selecionar resultado e revisar os dados de edição.
- Cancelar uma consulta e navegar; nenhum resultado atrasado deve sobrescrever outra tela.
- Buscar sem internet: formulário deve continuar editável e salvável.
- Exportar backup com livro, capa, duas pessoas e histórico ativo/devolvido.
- Alterar a biblioteca, restaurar o backup e conferir todos os registros e capa.
- Tentar restaurar JSON inválido, versão desconhecida, referências inexistentes e empréstimos ativos duplicados. Os dados atuais devem ser preservados.
- Testar cancelamento do seletor de arquivos e recusa da confirmação de restauração.
- Testar exportação para pasta local e, se disponível, provedor Drive/OneDrive.
- Conferir contraste, teclado, botões e navegação em tela pequena, fonte ampliada e Android 15.

Esses testes em Android não foram executados no ambiente de criação do ZIP.
