package br.com.literariamente.biblioapp;

import android.app.Activity;
import android.content.Context;
import com.android.billingclient.api.*;
import java.util.Collections;
import java.util.List;

/**
 * Compra única gerenciada pelo Google Play. Dados dos livros nunca saem do aparelho.
 * Cache local permite uso offline após consulta bem-sucedida ao Play.
 * Sem backend não é possível oferecer verificação antifraude forte ou revogação imediata offline.
 */
final class PremiumBilling implements PurchasesUpdatedListener {
    static final String PRODUCT_ID = "literariamente_premium";
    private static final String PREFS = "literariamente_premium_state";
    private static final String ENTITLED = "entitled";
    private final Activity activity;
    private final BillingClient client;
    private final Runnable changed;
    interface MessageListener { void show(String message); }
    interface ProductCallback { void accept(ProductDetails product); }
    private final MessageListener notify;
    private boolean owned;
    private boolean connected;
    private String formattedPrice = "Consulte a Google Play";

    PremiumBilling(Activity activity,Runnable changed,MessageListener notify){
        this.activity=activity;this.changed=changed;this.notify=notify;
        owned=activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ENTITLED,false);
        client=BillingClient.newBuilder(activity)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build();
        connect();
    }
    boolean hasPremium(){return owned;}
    String price(){return formattedPrice;}
    void close(){client.endConnection();}
    private void setOwned(boolean value){
        if(owned==value)return;
        owned=value;
        activity.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ENTITLED,value).apply();
        activity.runOnUiThread(changed);
    }
    private void connect(){
        client.startConnection(new BillingClientStateListener(){
            @Override public void onBillingSetupFinished(BillingResult result){
                connected=result.getResponseCode()==BillingClient.BillingResponseCode.OK;
                if(connected){refresh();loadPrice(null);}
            }
            @Override public void onBillingServiceDisconnected(){connected=false;}
        });
    }
    void refresh(){
        if(!connected)return; // Não revogar compra por falta de conexão.
        QueryPurchasesParams params=QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build();
        client.queryPurchasesAsync(params,(result,purchases)->{
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK)return;
            boolean found=false;
            for(Purchase p:purchases){
                if(p.getProducts().contains(PRODUCT_ID)){
                    if(p.getPurchaseState()==Purchase.PurchaseState.PURCHASED){
                        found=true;
                        acknowledge(p);
                    }else if(p.getPurchaseState()==Purchase.PurchaseState.PENDING){
                        activity.runOnUiThread(()->notify.show("Pagamento pendente. O Premium será liberado após a confirmação."));
                    }
                }
            }
            setOwned(found); // Apenas resposta válida do Google Play altera a licença.
        });
    }
    private void acknowledge(Purchase purchase){
        if(purchase.isAcknowledged())return;
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build(),
            result->{
                if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK)
                    activity.runOnUiThread(()->notify.show("Não foi possível confirmar a compra. Verifique sua conexão."));
            });
    }
    private void loadPrice(ProductCallback done){
        if(!connected){
            if(done!=null)activity.runOnUiThread(()->notify.show("Google Play indisponível. Verifique sua conexão e tente novamente."));
            return;
        }
        QueryProductDetailsParams params=QueryProductDetailsParams.newBuilder()
            .setProductList(Collections.singletonList(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()))
            .build();
        client.queryProductDetailsAsync(params,(result,details)->{
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK || details.getProductDetailsList().isEmpty()){
                if(done!=null)activity.runOnUiThread(()->notify.show("Produto Premium indisponível na Google Play."));
                return;
            }
            ProductDetails product=details.getProductDetailsList().get(0);
            List<ProductDetails.OneTimePurchaseOfferDetails> offers=product.getOneTimePurchaseOfferDetailsList();
            if(offers==null||offers.isEmpty()){
                if(done!=null)activity.runOnUiThread(()->notify.show("Oferta Premium indisponível."));
                return;
            }
            formattedPrice=offers.get(0).getFormattedPrice();
            if(done!=null)activity.runOnUiThread(()->done.accept(product));
            else activity.runOnUiThread(changed);
        });
    }
    void buy(){
        if(owned){notify.show("O Premium já está ativo nesta conta.");return;}
        loadPrice(product->{
            List<ProductDetails.OneTimePurchaseOfferDetails> offers=product.getOneTimePurchaseOfferDetailsList();
            if(offers==null||offers.isEmpty())return;
            BillingFlowParams.ProductDetailsParams item=BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .setOfferToken(offers.get(0).getOfferToken())
                .build();
            BillingResult launched=client.launchBillingFlow(activity,BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(item)).build());
            if(launched.getResponseCode()!=BillingClient.BillingResponseCode.OK)
                notify.show("Não foi possível iniciar a compra na Google Play.");
        });
    }
    @Override public void onPurchasesUpdated(BillingResult result,List<Purchase> purchases){
        if(result.getResponseCode()==BillingClient.BillingResponseCode.OK){refresh();}
        else if(result.getResponseCode()!=BillingClient.BillingResponseCode.USER_CANCELED)
            activity.runOnUiThread(()->notify.show("A compra não foi concluída. Verifique a Google Play."));
    }
}
