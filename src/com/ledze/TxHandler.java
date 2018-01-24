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
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        // 1.
        boolean valid;
        valid = pool.getAllUTXO().containsAll(tx.getOutputs());
        if ( !valid ) return false;

        // 2.
        valid = tx.getInputs()
                .parallelStream()
                .anyMatch(input -> !Crypto.verifySignature( tx.getOutput(input.outputIndex).address,
                                                            tx.getRawDataToSign(input.outputIndex),
                                                            input.signature) );
        if( valid ) return false;

        //3.
        for(UTXO utxo : pool.getAllUTXO()){
            long howmany = tx.getOutputs()
                    .stream()
                    .filter(x -> x.equals(pool.getTxOutput(utxo)))
                    .count();
            System.out.println("howmany:" + howmany);
            if(howmany > 1) {
                valid = false;
            }else {
                valid = true;
            }
        }
        if ( !valid ) return false;

        //4.
        for(Transaction.Output o : tx.getOutputs()){
            if ( isNegative(o.value) ) {
                valid = false;
                break;
            };
        }
        if ( !valid ) return false;

        //5.
        double inputValSum = tx.getInputs()
                .stream()
                .flatMapToDouble(i -> DoubleStream.of(tx.getOutput(i.outputIndex).value) )
                .sum();

        double outputValSum = tx.getOutputs()
                .stream()
                .flatMapToDouble(o -> DoubleStream.of(o.value) )
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
        Transaction[] t = null;
        // IMPLEMENT THIS
        ArrayList<Transaction> l = new ArrayList<>();
        for ( int i = 0 ; i<possibleTxs.length ; i++){
            if(isValidTx(possibleTxs[i])){
                l.add(possibleTxs[i]);
            }
        }


        return t;
    }

}
