package zapsolutions.zap.transactionHistory.transactionDetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.lightningnetwork.lnd.lnrpc.Transaction;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import zapsolutions.zap.R;
import zapsolutions.zap.customView.BSDScrollableMainView;
import zapsolutions.zap.fragments.ZapBSDFragment;
import zapsolutions.zap.util.BlockExplorer;
import zapsolutions.zap.util.ClipBoardUtil;
import zapsolutions.zap.util.MonetaryUtil;
import zapsolutions.zap.util.TimeFormatUtil;
import zapsolutions.zap.util.Wallet;
import zapsolutions.zap.util.ZapLog;

public class OnChainTransactionDetailBSDFragment extends ZapBSDFragment {

    public static final String TAG = OnChainTransactionDetailBSDFragment.class.getName();
    public static final String ARGS_TRANSACTION = "TRANSACTION";

    private BSDScrollableMainView mBSDScrollableMainView;
    private Transaction mTransaction;
    private TextView mChannelLabel;
    private TextView mChannel;
    private TextView mEventLabel;
    private TextView mEvent;
    private TextView mAmountLabel;
    private TextView mAmount;
    private TextView mFeeLabel;
    private TextView mFee;
    private TextView mDateLabel;
    private TextView mDate;
    private TextView mTransactionIDLabel;
    private TextView mTransactionID;
    private ImageView mTransactionIDCopyButton;
    private TextView mAddressLabel;
    private TextView mAddress;
    private TextView mConfrimationsLabel;
    private TextView mConfirmations;
    private ImageView mAddressCopyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bsd_on_chain_transaction_detail, container);

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mChannelLabel = view.findViewById(R.id.channelLabel);
        mChannel = view.findViewById(R.id.channel);
        mEventLabel = view.findViewById(R.id.eventLabel);
        mEvent = view.findViewById(R.id.event);
        mAmountLabel = view.findViewById(R.id.amountLabel);
        mAmount = view.findViewById(R.id.amount);
        mFeeLabel = view.findViewById(R.id.feeLabel);
        mFee = view.findViewById(R.id.fee);
        mDateLabel = view.findViewById(R.id.dateLabel);
        mDate = view.findViewById(R.id.date);
        mTransactionIDLabel = view.findViewById(R.id.transactionIDLabel);
        mTransactionID = view.findViewById(R.id.transactionID);
        mTransactionIDCopyButton = view.findViewById(R.id.txIDCopyIcon);
        mAddressLabel = view.findViewById(R.id.addressLabel);
        mAddress = view.findViewById(R.id.address);
        mAddressCopyButton = view.findViewById(R.id.addressCopyIcon);
        mConfrimationsLabel = view.findViewById(R.id.confirmationsLabel);
        mConfirmations = view.findViewById(R.id.confirmations);

        mBSDScrollableMainView.setSeparatorVisibility(true);
        mBSDScrollableMainView.setOnCloseListener(this::dismiss);

        if (getArguments() != null) {
            ByteString transactionString = (ByteString) getArguments().getSerializable(ARGS_TRANSACTION);

            try {
                bindOnChainTransaction(transactionString);

            } catch (InvalidProtocolBufferException | NullPointerException exception) {
                ZapLog.d(TAG, "Failed to parse transaction.");
                dismiss();
            }
        }

        return view;
    }


    private void bindOnChainTransaction(ByteString transactionString) throws InvalidProtocolBufferException {
        mTransaction = Transaction.parseFrom(transactionString);

        String channelLabel = getString(R.string.channel) + ":";
        mChannelLabel.setText(channelLabel);
        String eventLabel = getString(R.string.event) + ":";
        mEventLabel.setText(eventLabel);
        String amountLabel = getString(R.string.amount) + ":";
        mAmountLabel.setText(amountLabel);
        String feeLabel = getString(R.string.fee) + ":";
        mFeeLabel.setText(feeLabel);
        String dateLabel = getString(R.string.date) + ":";
        mDateLabel.setText(dateLabel);
        String transactionIDLabel = getString(R.string.transactionID) + ":";
        mTransactionIDLabel.setText(transactionIDLabel);
        String addressLabel = getString(R.string.address) + ":";
        mAddressLabel.setText(addressLabel);
        String confirmationsLabel = getString(R.string.confirmations) + ":";
        mConfrimationsLabel.setText(confirmationsLabel);

        mDate.setText(TimeFormatUtil.formatTimeAndDateLong(mTransaction.getTimeStamp(), getActivity()));

        mAddress.setText(mTransaction.getDestAddresses(0));
        mTransactionID.setText(mTransaction.getTxHash());

        mTransactionID.setOnClickListener(view -> new BlockExplorer().showTransaction(mTransaction.getTxHash(), getActivity()));
        mAddress.setOnClickListener(view -> new BlockExplorer().showAddress(mTransaction.getDestAddresses(0), getActivity()));
        mTransactionIDCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "TransactionID", mTransaction.getTxHash()));
        mAddressCopyButton.setOnClickListener(view -> ClipBoardUtil.copyToClipboard(getContext(), "Address", mTransaction.getDestAddresses(0)));
        if (mTransaction.getNumConfirmations() < 7) {
            mConfirmations.setText(String.valueOf(mTransaction.getNumConfirmations()));
        } else {
            mConfirmations.setText("6+");
        }


        // is internal?
        if (Wallet.getInstance().isTransactionInternal(mTransaction)) {
            bindInternal();
        } else {
            bindNormalTransaction();
        }
    }

    private void bindInternal() {
        mBSDScrollableMainView.setTitle(R.string.channel_event);
        Long amount = mTransaction.getAmount();

        mAddress.setVisibility(View.GONE);
        mAddressLabel.setVisibility(View.GONE);
        mAddressCopyButton.setVisibility(View.GONE);
        mAmount.setVisibility(View.GONE);
        mAmountLabel.setVisibility(View.GONE);

        if (mTransaction.getTotalFees() > 0) {
            mFee.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(mTransaction.getTotalFees()));
        } else {
            mFee.setVisibility(View.GONE);
            mFeeLabel.setVisibility(View.GONE);
        }

        String alias = Wallet.getInstance().getNodeAliasFromChannelTransaction(mTransaction, getActivity());
        mChannel.setText(alias);

        switch (amount.compareTo(0L)) {
            case 0:
                // amount = 0
                mEvent.setText(R.string.force_closed_channel);
                break;
            case 1:
                // amount > 0 (Channel closed)
                mEvent.setText(R.string.closed_channel);
                break;
            case -1:
                // amount < 0 (Channel opened)
                mEvent.setText(R.string.opened_channel);
                break;
        }
    }

    private void bindNormalTransaction() {
        mBSDScrollableMainView.setTitle(R.string.transaction_detail);
        mChannel.setVisibility(View.GONE);
        mChannelLabel.setVisibility(View.GONE);
        mEvent.setVisibility(View.GONE);
        mEventLabel.setVisibility(View.GONE);
        mAmount.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(mTransaction.getAmount()).replace("-", "- "));

        Long amount = mTransaction.getAmount();

        switch (amount.compareTo(0L)) {
            case 0:
                // amount = 0 (should actually not happen)
                mFee.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(mTransaction.getTotalFees()));
                break;
            case 1:
                // amount > 0 (received on-chain)
                mFee.setVisibility(View.GONE);
                mFeeLabel.setVisibility(View.GONE);
                break;
            case -1:
                // amount < 0 (sent on-chain)
                mFee.setText(MonetaryUtil.getInstance().getPrimaryDisplayAmountAndUnit(mTransaction.getTotalFees()));
                break;
        }
    }
}
