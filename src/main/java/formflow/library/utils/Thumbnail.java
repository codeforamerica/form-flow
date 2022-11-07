//package formflow.library.utils;
//
//import static java.util.Objects.requireNonNull;
//
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Base64;
//import javax.imageio.ImageIO;
//import net.coobird.thumbnailator.Thumbnails;
//import org.springframework.web.multipart.MultipartFile;
//
//public class Thumbnail {
//
//  /**
//   * Takes a multipart file image and generates a PNG thumbnail to be saved to cloud storage for persistence with file uploads
//   *
//   * @param file multipart image file to be converted to a thumbnail
//   * @return base64 encoded string of the PNG thumbnail image
//   * @throws IOException
//   */
//
//  public static String generate(MultipartFile file) throws IOException {
//    Path paths = Files.createTempDirectory("");
//    File thumbFile = new File(paths.toFile(),
//        requireNonNull(requireNonNull(file.getOriginalFilename())));
//    FileOutputStream fos = new FileOutputStream(thumbFile);
//    fos.write(file.getBytes());
//    fos.close();
//    ByteArrayOutputStream os = new ByteArrayOutputStream();
//    BufferedImage outputImage = Thumbnails.of(thumbFile).size(300, 300).asBufferedImage();
//    ImageIO.write(outputImage, "png", os);
//    String thumbDataURL = "data:image/png;base64," + Base64.getEncoder().encodeToString(os.toByteArray());
//    outputImage.flush();
//    thumbFile.delete();
//    Files.delete(paths);
//    return thumbDataURL;
//  }
//}
