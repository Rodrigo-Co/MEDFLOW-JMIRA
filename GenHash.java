public class GenHash {
  public static void main(String[] args) {
    System.out.println(org.mindrot.jbcrypt.BCrypt.hashpw("medflow123", org.mindrot.jbcrypt.BCrypt.gensalt(10)));
  }
}
