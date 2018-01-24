package com.ledze;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class TxHandler {
    private UTXOPool pool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        pool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if: </br>
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, </br>
     * (2) the signatures on each input of {@code tx} are valid, </br>
     * (3) no UTXO is claimed multiple times by {@code tx}, </br>
     * (4) all of {@code tx}s output values are non-negative, and </br>
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise. </br>
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // 1.
        boolean valid = true;
        UTXO _utxo = null;
        for(Transaction.Input input : tx.getInputs() ){
            _utxo = new UTXO(tx.getHash(), input.outputIndex);
            valid = pool.getAllUTXO().contains(_utxo);
            if( !valid )break;
        }
        if( !valid ) return false;

        // 2.
        valid = tx.getInputs()
                .parallelStream()
                .anyMatch(input -> !Crypto.verifySignature(tx.getOutput(input.outputIndex).address,
                        tx.getRawDataToSign(input.outputIndex),
                        input.signature));
        if (valid) return false;

        //3.
        for (UTXO utxo : pool.getAllUTXO()) {
            long howmany = tx.getOutputs()
                    .stream()
                    .filter(x -> x.equals(pool.getTxOutput(utxo)))
                    .count();
            System.out.println("howmany:" + howmany);
            if (howmany > 1) {
                valid = false;
            } else {
                valid = true;
            }
        }
        if (!valid) return false;

        //4.
        for (Transaction.Output o : tx.getOutputs()) {
            if (isNegative(o.value)) {
                valid = false;
                break;
            }
            ;
        }
        if (!valid) return false;

        //5.
        double inputValSum = tx.getInputs()
                .stream()
                .flatMapToDouble(i -> DoubleStream.of(tx.getOutput(i.outputIndex).value))
                .sum();

        double outputValSum = tx.getOutputs()
                .stream()
                .flatMapToDouble(o -> DoubleStream.of(o.value))
                .sum();

        return inputValSum >= outputValSum;

    }

    private static boolean isNegative(double number) {
        return (Double.doubleToLongBits(number) & Long.MIN_VALUE) == Long.MIN_VALUE;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> l = new ArrayList<>();
        ArrayList<Transaction.Output> lstOutputs = new ArrayList<>();
        for (int i = 0; i < possibleTxs.length; i++) {
            boolean doubleSpending = false;
            if (isValidTx(possibleTxs[i])) {

                if (lstOutputs.isEmpty()) {
                    ArrayList<Transaction.Output> lstFirstOutputs = new ArrayList<>();
                    for( Transaction.Output o : possibleTxs[i].getOutputs() ){
                        if(lstFirstOutputs.isEmpty()){
                            lstFirstOutputs.add(o);
                        }else{
                            if( !lstFirstOutputs.contains(o) ){
                                lstFirstOutputs.add(o);
                            }
                        }
                    }
                    lstOutputs.addAll(lstFirstOutputs);
                } else {
                    for (Transaction.Output output : possibleTxs[i].getOutputs()) {
                        doubleSpending = lstOutputs.contains(output);
                        if (doubleSpending) {
                            break;
                        }
                    }
                    if (doubleSpending) {
                        System.out.println("WARNING!!!: Double spending, the Tx will not be added to the ledger.");
                        continue;
                    }
                }
                l.add(possibleTxs[i]);
            }
        }

        for(Transaction tx: l){
            byte[] txHash = tx.getHash();

            for(Transaction.Input input : tx.getInputs()){
                pool.addUTXO(
                        new UTXO(txHash, input.outputIndex),
                        tx.getOutput(input.outputIndex)
                );
            }
        }

        return l.stream().toArray(Transaction[]::new);
    }

   /* public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            boolean doubleSpending = false;
            System.out.println("i: "+i);
            for (int j = 0; j < 10; j++) {
                System.out.println("j: "+j);
                doubleSpending = (j==8);
                if(doubleSpending)break;
            }
            if(doubleSpending){
                System.out.println("double spending");
                break;
            }
        }

    }*/

}
