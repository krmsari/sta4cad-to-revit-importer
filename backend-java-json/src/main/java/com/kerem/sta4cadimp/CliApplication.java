package com.kerem.sta4cadimp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kerem.sta4cadimp.entity.Project;
import com.kerem.sta4cadimp.service.St4FileParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;

public class CliApplication {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("HATA: Gerekli argümanlar sağlanmadı.");
            System.err.println("Kullanım: java -jar sta4cad-imp.jar <girdi.st4> <çıktı.json>");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputFilePath = args[1];

        System.out.println("Girdi dosyası işleniyor: " + inputFilePath);
        System.out.println("Çıktı dosyası oluşturulacak: " + outputFilePath);

        try (InputStream inputStream = new FileInputStream(inputFilePath)) {

            St4FileParser st4FileParser = new St4FileParser();


            Project project = st4FileParser.parse(inputStream, new File(inputFilePath).getName());

            if (project != null) {

                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(outputFilePath).toFile(), project);

                System.out.println("İşlem başarılı. JSON dosyası oluşturuldu: " + outputFilePath);
                System.exit(0); // Başarılı çıkış kodu
            } else {
                System.err.println("HATA: .st4 dosyası ayrıştırılamadı.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("İşlem sırasında kritik bir hata oluştu: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
