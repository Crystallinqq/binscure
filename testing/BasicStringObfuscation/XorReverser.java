class XorReverser {
    private static final String correctPassword = "aRa2lPT6A6gIqm4RE";
    
    public static void main(final String[] array) {
        System.out.println(xor(correctPassword));
    }
    
    private static String xor(final String s) {
        final char[] charArray = s.toCharArray();
        final char[] array = new char[charArray.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = (char)(charArray[i] ^ i % 3);
        }
        return new String(array);
    }
}