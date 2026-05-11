public class CheckHash {
  public static void main(String[] args) {
    String hash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    System.out.println(org.mindrot.jbcrypt.BCrypt.checkpw("medflow123", hash));
    System.out.println(org.mindrot.jbcrypt.BCrypt.checkpw("password", hash));
    System.out.println(org.mindrot.jbcrypt.BCrypt.checkpw("123456", hash));
  }
}
