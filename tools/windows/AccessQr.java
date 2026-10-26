// Local QR only. Uses ZXing already declared by PING; no download, token or session.
import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import java.net.URI;
public class AccessQr {
    public static void main(String[] args) throws Exception {
        var uri = URI.create(args[0]);
        if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Only a public access origin is allowed");
        var bits = new QRCodeWriter().encode(args[0], BarcodeFormat.QR_CODE, 256, 256);
        var svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 256 256\"><rect width=\"256\" height=\"256\" fill=\"white\"/><path fill=\"black\" d=\"");
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++)
            if (bits.get(x, y)) svg.append("M").append(x).append(" ").append(y).append("h1v1h-1z");
        System.out.print(svg.append("\"/></svg>"));
    }
}
