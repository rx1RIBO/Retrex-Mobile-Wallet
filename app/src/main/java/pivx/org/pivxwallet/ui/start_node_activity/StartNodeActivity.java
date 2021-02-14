package rext.org.rextwallet.ui.start_node_activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Spinner;
import android.widget.Toast;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import global.PivtrumGlobalData;
import pivtrum.PivtrumPeer;
import pivtrum.PivtrumPeerData;
import rext.org.rextwallet.R;
import rext.org.rextwallet.module.RextContext;
import rext.org.rextwallet.ui.base.BaseActivity;
import rext.org.rextwallet.ui.pincode_activity.PincodeActivity;
import rext.org.rextwallet.ui.wallet_activity.WalletActivity;
import rext.org.rextwallet.utils.DialogBuilder;
import rext.org.rextwallet.utils.DialogsUtil;

import static global.PivtrumGlobalData.FURSZY_TESTNET_SERVER;

/**
 * Created by Neoperol on 6/27/17.
 */

public class StartNodeActivity extends BaseActivity {

    private Button openDialog;
    private Button btnSelectNode;
    private Spinner dropdown;
    private ArrayAdapter<String> adapter;
    private List<String> hosts = new ArrayList<>();

    private AtomicBoolean isLoading = new AtomicBoolean(false);

    private Handler handler;

    private static final List<PivtrumPeerData> trustedNodes = PivtrumGlobalData.listTrustedHosts(
            RextContext.NETWORK_PARAMETERS,
            RextContext.NETWORK_PARAMETERS.getPort()
    );

    @Override
    protected void onCreateView(Bundle savedInstanceState, ViewGroup container) {

        getLayoutInflater().inflate(R.layout.fragment_start_node, container);
        setTitle(R.string.select_node);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        handler = new Handler();

        // Open Dialog
        openDialog = (Button) findViewById(R.id.openDialog);
        openDialog.setOnClickListener(view -> {
            DialogBuilder dialogBuilder = DialogsUtil.buildtrustedNodeDialog(StartNodeActivity.this, pivtrumPeerData -> {
                if(!trustedNodes.contains(pivtrumPeerData)) {
                    dropdown.setAdapter(null);
                    adapter.clear();
                    hosts = new ArrayList<>();
                    trustedNodes.add(pivtrumPeerData);
                    for (PivtrumPeerData trustedNode : trustedNodes) {
                        hosts.add(trustedNode.getHost());
                    }
                    adapter.addAll(hosts);
                    dropdown.setAdapter(adapter);
                    dropdown.setSelection(hosts.size() - 1);
                }
            });
            dialogBuilder.show();
        });
        findViewById(R.id.btn_default).setOnClickListener(v -> {
            try {
                if (isLoading.compareAndSet(false, true)) {
                    rextApplication.setTrustedServer(null);
                    rextApplication.getWalletConfiguration().setDSNDiscovery(false);
                    if (rextModule.isStarted()) {
                        rextApplication.stopBlockchain();
                        // now that everything is good, start the service
                        handler.postDelayed(() -> rextApplication.startRextService(), TimeUnit.SECONDS.toMillis(5));
                    }
                    goNext();
                    finish();
                }
            } catch (Exception e){
                LoggerFactory.getLogger(StartNodeActivity.class).error("Error touching default nodes button", e);
            } finally {
                isLoading.set(false);
            }
        });

        findViewById(R.id.btn_dns_discovery).setOnClickListener(v -> {
            try {
                if (isLoading.compareAndSet(false, true)) {
                    rextApplication.setTrustedServer(null);
                    rextApplication.getWalletConfiguration().setDSNDiscovery(true);
                    if (rextModule.isStarted()) {
                        rextApplication.stopBlockchain();
                        // now that everything is good, start the service
                        handler.postDelayed(() -> rextApplication.startRextService(), TimeUnit.SECONDS.toMillis(5));
                    }
                    Toast.makeText(v.getContext(), R.string.dns_discovery_enabled, Toast.LENGTH_SHORT).show();
                    goNext();
                    finish();
                }
            } catch (Exception e){
                LoggerFactory.getLogger(StartNodeActivity.class).error("Error touching DNS discovery nodes button", e);
            } finally {
                isLoading.set(false);
            }
        });

        // Node selected
        btnSelectNode = findViewById(R.id.btnSelectNode);
        btnSelectNode.setOnClickListener(v -> {
            if (isLoading.compareAndSet(false, true)) {
                int selected = dropdown.getSelectedItemPosition();
                PivtrumPeerData selectedNode = trustedNodes.get(selected);
                boolean isStarted = rextApplication.getAppConf().getTrustedNode() != null;
                rextApplication.setTrustedServer(selectedNode);
                rextApplication.getWalletConfiguration().setDSNDiscovery(false);

                if (isStarted) {
                    rextApplication.stopBlockchain();
                    // now that everything is good, start the service
                    new Handler().postDelayed(() -> rextApplication.startRextService(), TimeUnit.SECONDS.toMillis(5));
                }
                goNext();
                isLoading.set(false);
                finish();
            }else {
                Toast.makeText(v.getContext(), R.string.app_process_task, Toast.LENGTH_SHORT).show();
            }
        });

        dropdown = (Spinner)findViewById(R.id.spinner);

        // add connected node if it's not on the list
        PivtrumPeerData pivtrumPeer = rextApplication.getAppConf().getTrustedNode();
        if (pivtrumPeer != null && !trustedNodes.contains(pivtrumPeer)){
            trustedNodes.add(pivtrumPeer);
        }

        int selectionPos = 0;

        for (int i=0;i<trustedNodes.size();i++){
            PivtrumPeerData trustedNode = trustedNodes.get(i);
            if (pivtrumPeer!=null && pivtrumPeer.getHost().equals(trustedNode.getHost())){
                selectionPos = i;
            }
            if (trustedNode.getHost().equals(FURSZY_TESTNET_SERVER)){
                hosts.add("pivt.furszy.tech");
            }else
                hosts.add(trustedNode.getHost());
        }
        adapter = new ArrayAdapter<>(this, R.layout.spinner_dropdown_white, hosts);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        dropdown.setAdapter(adapter);
        dropdown.setSelection(selectionPos);
    }

    private void goNext() {
        Class clazz = null;
        if (rextApplication.getAppConf().getPincode() == null){
            clazz = PincodeActivity.class;
        }else {
            clazz = WalletActivity.class;
        }
        Intent intent = new Intent(this, clazz);
        startActivity(intent);
    }

    public static int convertDpToPx(Resources resources, int dp){
        return Math.round(dp*(resources.getDisplayMetrics().xdpi/ DisplayMetrics.DENSITY_DEFAULT));
    }

}
