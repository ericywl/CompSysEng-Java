import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Cert {
    public static void main(String[] args) {
        try {
            InputStream fis = new FileInputStream("files/CA.crt");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate CAcert = (X509Certificate) cf.generateCertificate(fis);
            PublicKey CAkey = CAcert.getPublicKey();



        } catch (FileNotFoundException | CertificateException e) {
            e.printStackTrace();
        }
    }
}
