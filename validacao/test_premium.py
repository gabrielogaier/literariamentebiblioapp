from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
main = (ROOT / "app/src/main/java/br/com/literariamente/biblioapp/MainActivity.java").read_text()
billing = (ROOT / "app/src/main/java/br/com/literariamente/biblioapp/PremiumBilling.java").read_text()
gradle = (ROOT / "app/build.gradle").read_text()

assert "billing:9.1.0" in gradle
assert 'providers.gradleProperty("premiumEnforcement").orElse("false")' in gradle, "Premium deve ficar desligado por padrão"
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
assert "if(!connected){connect();return;}" in billing
print("Regras estáticas do Premium OK (testes de checkout em Android/Play Console ainda necessários).")
