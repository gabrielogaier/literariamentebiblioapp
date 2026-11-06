from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
main = (ROOT / "app/src/main/java/br/com/literariamente/biblioapp/MainActivity.java").read_text()
billing = (ROOT / "app/src/main/java/br/com/literariamente/biblioapp/PremiumBilling.java").read_text()
gradle = (ROOT / "app/build.gradle").read_text()

assert "billing:9.1.0" in gradle
assert 'providers.gradleProperty("premiumEnforcement").orElse("true")' in gradle, "Premium deve ficar ligado por padrão"
assert gradle.count("'PREMIUM_ENFORCEMENT_ENABLED', String.valueOf(premiumEnabled)") == 2
assert 'if(!requirePremium("empréstimos"))return;' in main
assert 'if(!requirePremium("cadastro de pessoas"))return;' in main
assert 'if(!requirePremium("gestão de empréstimos"))return;' in main
assert 'if(!requirePremium("histórico de empréstimos"))return;' in main
assert 'if(!requirePremium("restauração de backup"))return;' in main
assert 'if(request==42&&result==RESULT_OK&&!requirePremium("restauração de backup"))return;' in main
assert 'private void exportBackup(){Intent i=' in main, "Exportação deve continuar gratuita"
assert 'PRODUCT_ID = "literariamente_premium"' in billing
assert "Purchase.PurchaseState.PURCHASED" in billing
assert "client.acknowledgePurchase" in billing
assert "setOwned(found)" in billing
assert "void checkAccess(AccessCallback done)" in billing
assert "if(!connected){" in billing
assert "checkAccess(active->{" in billing
assert 'button(body,"Já comprei / restaurar compra",this::restorePremium);' in main
assert 'toast("Premium restaurado com sucesso!");' in main
assert 'toast("Nenhuma compra Premium foi encontrada na conta Google da Play Store.");' in main
assert 'toast("Não foi possível consultar a Google Play.' in main
print("Regras estáticas do Premium OK (testes de checkout em Android/Play Console ainda necessários).")
