import java.io.*;
import java.util.*;
import java.math.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Mine{
    
    public static final BigInteger MAX_TARGET = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
    public static final String STARTER_COINBASE = ";1333dGpHU6gQShR596zbKHXEeSihdtoyLb>";
    static TreeMap<Integer, TreeMap<Integer, ArrayList<String>>> transactionsByFee;
    static ArrayList<String> transactionsAdded;
    static int totalEarned;
    
    public static void main(String[] args) throws Exception{
        
        long timestamp = Instant.now().toEpochMilli();
        
        String file = args[0];
        String difficulty = args[1];
        String prevHash = args[2];
        
        int no = Integer.parseInt(difficulty);
        String hex = Integer.toHexString(no);
        
        BigInteger difficultyHex = new BigInteger(hex, 16);
        
        BigInteger target = MAX_TARGET.divide(difficultyHex);
        
        BufferedReader infile = new BufferedReader(new FileReader(file));
         
        transactionsByFee = new TreeMap<>();
        transactionsAdded = new ArrayList<>();
        totalEarned = 50;
        
        int inputsAndOutputs=0;
        int potentialEarnings=50;
        ArrayList<String> allTransactions = new ArrayList<>();
        while(infile.ready()){
            String transaction = infile.readLine().trim();
            String[] transactionArray = transaction.split(";");
            String[] inputs = transactionArray[0].split(",");
            String[] outputs = transactionArray[1].split(",");
            int inputTotal = 0;
            for(int i = 0; i<inputs.length; i++){
                String[] values = inputs[i].split(">");
                inputTotal += Integer.parseInt(values[1]);
            }
            int outputTotal = 0;
            for(int i = 0; i<outputs.length; i++){
                String[] values = outputs[i].split(">");
                outputTotal += Integer.parseInt(values[1]);
            }
            int fee = inputTotal-outputTotal;
            int size = inputs.length+outputs.length;
            insertIntoMap(transaction,fee,size);
            inputsAndOutputs+=size;
            potentialEarnings+=fee;
            allTransactions.add(transaction);
        }
        infile.close();
        
        int blockSize = 15;
        
        if(inputsAndOutputs<=15){
            transactionsAdded = allTransactions;
            totalEarned = potentialEarnings;
            blockSize = inputsAndOutputs+1;
        }
        else{
            while(blockSize!=0){
                int size = findNextBest(blockSize);
                if(size==0){
                    break;
                }
                blockSize = blockSize - size;
            }
            blockSize = 16 - blockSize;
        }
        String coinbase = STARTER_COINBASE+totalEarned;
        transactionsAdded.add(coinbase);
        String concatRoot = "";
        for(String s:transactionsAdded){
            concatRoot+=s;
        }
        String concatRootHash = calculateHash(concatRoot);
        char x = 32;
        int counter = 1;
        String nonce = String.valueOf(new char[]{x,x,x,x});
        String blockHash = "";
        while(true){
            String block = prevHash+blockSize+timestamp+difficulty+nonce+concatRootHash;
            blockHash = calculateHash(block);
            BigInteger blockHashHex = new BigInteger(blockHash, 16);
            if(blockHashHex.compareTo(target) == -1){
                break;
            }
            else{
                nonce = incrementStringNonce(nonce,counter);
                counter++;
            }
        }
        
        printBlock(blockHash, prevHash, blockSize, timestamp, difficulty, nonce, concatRootHash, transactionsAdded);

    }
    
    private static void insertIntoMap(String transaction, int fee, int size){
        if(transactionsByFee.containsKey(fee)){
            TreeMap<Integer, ArrayList<String>> transactionsBySize= transactionsByFee.get(fee);
            if(transactionsBySize.containsKey(size)){
                ArrayList<String> transactions = transactionsBySize.get(size);
                transactions.add(transaction);
                transactionsBySize.put(size, transactions);
                transactionsByFee.put(fee, transactionsBySize);
            }
            else{
                ArrayList<String> transactions = new ArrayList<>();
                transactions.add(transaction);
                transactionsBySize.put(size, transactions);
                transactionsByFee.put(fee, transactionsBySize);
            }
        }
        else{
            TreeMap<Integer, ArrayList<String>> transactionsBySize = new TreeMap<>();
            ArrayList<String> transactions = new ArrayList<>();
            transactions.add(transaction);
            transactionsBySize.put(size, transactions);
            transactionsByFee.put(fee, transactionsBySize);
            transactionsByFee.put(fee, transactionsBySize);
        }
    }
    
    private static int findNextBest(int maxSize){
        TreeSet<Integer> set = new TreeSet<Integer>(transactionsByFee.keySet());
        for(Integer i:set.descendingSet()){
            TreeMap<Integer, ArrayList<String>> transactionsBySize = transactionsByFee.get(i);
            for(Integer j:transactionsBySize.keySet()){
                if(j<=maxSize){
                    ArrayList<String> transactions = transactionsBySize.get(j);
                    String transaction = transactions.get(0);
                    transactions.remove(0);
                    if(transactions.isEmpty()){
                        transactionsBySize.remove(j);
                        if(transactionsBySize.isEmpty()){
                            transactionsByFee.remove(i);
                        }
                        else{
                            transactionsByFee.put(i, transactionsBySize);
                        }
                    }
                    else{
                        transactionsBySize.put(j, transactions);
                        transactionsByFee.put(i, transactionsBySize);
                    }
                    transactionsAdded.add(transaction);
                    totalEarned+=i;
                    return j;
                }
            }
        }
        return 0;
    }
    
    
    private static String calculateHash(String x) {
        if (x == null) {
            return "0";
        }
        byte[] hash = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(x.getBytes());
        } catch (NoSuchAlgorithmException nsaex) {
            System.err.println("No SHA-256 algorithm found.");
            System.err.println("This generally should not happen...");
            System.exit(1);
        }
        return convertBytesToHexString(hash);

    }
    
    private static String convertBytesToHexString(byte[] bytes) {
        StringBuffer toReturn = new StringBuffer();
        for (int j = 0; j < bytes.length; j++) {
            String hexit = String.format("%02x", bytes[j]);
            toReturn.append(hexit);
        }
        return toReturn.toString();
    }
    
    private static String incrementStringNonce(String nonce, int counter) {
        char[] charArray = nonce.toCharArray();
        if(counter%857375==0){
            charArray[0]+=1;
            if(charArray[0]%127==0){
                return "";
            }
            charArray[1]=32;
            charArray[2]=32;
            charArray[3]=32;
        }
        else if(counter%9025==0){
            charArray[1]+=1;
            if(charArray[1]%127==0){
                charArray[0]+=1;
                charArray[1]=32;
            }
            charArray[2]=32;
            charArray[3]=32;
        }
        else if(counter%95==0){
            charArray[2]+=1;
            if(charArray[2]%127==0){
                charArray[1]+=1;
                charArray[2]=32;
            }
            charArray[3]=32;
        }
        else{
            charArray[3]+=1;
            if(charArray[3]%127==0){
                charArray[2]+=1;
                charArray[3]=32;
            }
        }
        return new String(charArray);
    }
    
    private static void printBlock(String blockHash, String prevHash, int blockSize, long timestamp, String difficulty, String nonce, String concatRootHash, ArrayList<String> transactions){
        System.out.println("CANDIDATE BLOCK = Hash "+blockHash);
        System.out.println("---");
        System.out.println(prevHash);
        System.out.println(blockSize);
        System.out.println(timestamp);
        System.out.println(nonce);
        System.out.println(concatRootHash);
        for(String s: transactions){
            System.out.println(s);
        }
        
    }


}