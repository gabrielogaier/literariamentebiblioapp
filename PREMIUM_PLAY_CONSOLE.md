# Premium e publicação no Google Play

## Modelo

Um único aplicativo Android, biblioteca offline e compra única não consumível. Não existem contas ou servidores do Literariamente. O Google Play gerencia pagamento, resgate dos códigos de cortesia e restauração da compra na mesma conta Google.

- Identificador exato do produto: `literariamente_premium`
- Tipo: **produto de compra única**, não consumível, com **opção de compra**.
- Preço desejado: **R$ 14,90 no Brasil**. O valor efetivo exibido é o que for cadastrado na Play Console.
- Gratuito: cadastro, edição, capa, avaliações, busca, desejos e exportar backup JSON.
- Premium: empréstimos, devoluções, pessoas, histórico e importar/restaurar backup JSON.
- Backups nunca são enviados pelo aplicativo a servidores. A exportação usa o seletor de documentos Android.

## Sequência segura de ativação

1. Concluir cadastro de desenvolvedor, perfil de pagamentos de comerciante e configuração do app na Play Console.
2. Criar e ativar o produto de compra única com ID **exatamente** `literariamente_premium`. Configurar o preço no Console; não o gravar no código.
3. Para testar, em **GitHub Actions > Build Android (APK e AAB) > Run workflow**, marcar `premium_teste=true` e **não** marcar `publicar_release`. Baixar o AAB assinado do artefato dessa execução e enviar à faixa de testes internos na Play Console. Configurar compradores de licença (license testers). Comprar com pagamento de teste, verificar estados PENDING/PURCHASED e restauração em outro aparelho.
4. Criar códigos promocionais individuais para os dois usuários antigos, ativar promoção e validar resgate em suas contas Google. Para campanha de dez primeiros, gerar dez códigos individuais, não um código genérico de dez usos.
5. Depois dos testes e das cortesias, preparar a ativação definitiva: alterar o padrão da propriedade Gradle `premiumEnforcement` de `false` para `true` em um PR específico, validar as migrações e lançar novo AAB. Não ativar antes de assegurar cortesias e compatibilidade de assinatura.
6. Antes da atualização com cobranças, recomendar exportar backup. Garantir que os usuários antigos resgatem os códigos antes de ativar bloqueios.

## Estado atual

A integração e a tela Premium estão preparadas. **A aplicação das restrições está desativada por padrão** nas builds de debug e release. Para gerar um AAB de teste com acesso controlado, usar somente o parâmetro `-PpremiumEnforcement=true` ou a opção `premium_teste` no workflow manual. Este modo de teste não publica GitHub Release. Enquanto estiver desativada, o usuário não pode realizar compras pela interface e os recursos existentes seguem disponíveis.

A licença é recuperada consultando o Google Play quando há conexão. Após confirmação, um registro local permite continuar usando o Premium offline. Sem backend próprio, uma compra reembolsada/revogada não pode ser detectada enquanto o dispositivo estiver offline e a validação no cliente é menos resistente a adulteração do que a verificação em servidor. Não inventar licença por data de instalação nem por presença de dados antigos.

A compra é reconhecida somente após status `PURCHASED`; compras pendentes não liberam acesso. As compras precisam ser confirmadas (`acknowledgePurchase`). Para visualizar preços e comprar, o APK precisa estar corretamente configurado no Google Play e assinado/publicado em faixa de testes ou produção.

## Plano de validação manual

- Instalador gratuito: cadastrar, listar, editar e exportar livros sem rede.
- Premium ativo: emprestar, devolver, pessoas, histórico e restaurar JSON sem rede depois da primeira consulta válida.
- Conta sem Premium: ao acessar empréstimos/pessoas/histórico/importação, mostrar tela de upgrade sem executar mutações no banco.
- Resgatar código promocional no Play, retornar ao app e confirmar atualização do acesso.
- Testar cancelamento de checkout e compra pendente sem conceder acesso.
- Trocar de aparelho/limpar dados, entrar na mesma conta Google e restaurar a compra.
- Simular falha de rede para garantir que não revogue direito confirmado por mero erro de consulta.
- Confirmar que restaurar backup de formato legado preserva atomicidade e que a exportação gratuita continua acessível.
- Testar reembolso e revogação com rede e observar remoção do direito; testar que dados antigos nunca sejam apagados.
- Confirmar assinatura de atualização do APK distribuído anteriormente e Play App Signing antes de migrar instalações.

## Segurança e privacidade

Não subir credenciais, tokens, comprovantes de compra, códigos promocionais, backups ou keystores no GitHub. Configuração de produtos e campanhas fica exclusivamente no Google Play Console. O cache local da licença é um compromisso explícito de um aplicativo 100% independente de servidor: não serve de prova antifraude.
