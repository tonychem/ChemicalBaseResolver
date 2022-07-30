import com.epam.indigo.Indigo;
import com.epam.indigo.IndigoException;
import com.epam.indigo.IndigoObject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChemicalDataBaseResolver {

    private Indigo indigo = new Indigo();

    private String[] headers = {"NAME", "FORMULA", "ALTERNATIVE_NAME", "ANOTHER_NAME", "OSTATOK",
            "KOMNATA", "SHKAFF", "POLKA", "KOMENT", "CAS", "Smiles"};
    private CSVFormat fmt = CSVFormat.Builder.create()
            .setDelimiter(";")
            .setIgnoreEmptyLines(true)
            .setIgnoreHeaderCase(true)
            .setHeader(headers)
            .build();


    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            System.out.print("Введите название csv файла (без расширения): ");
            String fileInputName = scanner.nextLine();

            while (!csvFileExists(fileInputName)) {
                System.out.println("Файла с именем " + fileInputName + ".csv" + " не существует. Попробуйте еще раз");
                fileInputName = scanner.nextLine();
            }

            var chemicalDataBaseResolver = new ChemicalDataBaseResolver();
            chemicalDataBaseResolver.run(fileInputName);
        }
    }

    public static boolean csvFileExists(String fileInputName) {
        return Files.exists(Path.of(fileInputName + ".csv").toAbsolutePath());
    }

    public void run(String fileInputName) throws IOException {
        List<Integer> errorStrings = mapper(fileInputName, fileInputName + "_" + LocalDate.now());
        if (errorStrings.isEmpty()) {
            System.out.println("Вся база была сконвертирована успешно");
        } else {
            System.out.println("База сконвертирована, кроме строк: ");
            for(Integer i : errorStrings) {
                System.out.println(i);
            }
        }
    }

    private List<CSVRecord> recordsAsList(String filename) throws IOException {
        List<CSVRecord> records;

        try (Reader rd = new FileReader(filename + ".csv")) {
            CSVParser parser = CSVParser.parse(rd, fmt);
            records = parser.getRecords();
        }
        return records;
    }

    public List<Integer> mapper(String fileInputName, String outputFileName) throws IOException {
        List<CSVRecord> allCsvRecords = recordsAsList(fileInputName);
        IndigoObject saver = indigo.writeFile(outputFileName + ".rdf");
        List<Integer> errors = new ArrayList<>();
        saver.rdfHeader();

        for (int i = 1; i < allCsvRecords.size(); i++) {
            try {
                CSVRecord record = allCsvRecords.get(i);
                IndigoObject mol = indigo.loadMolecule(record.get("Smiles"));
                mol = setProperties(record, mol, headers);
                mol.layout();
                mol.dearomatize();
                saver.rdfAppend(mol);
            } catch (IndigoException e) {
                errors.add(i + 1);
            }
        }

        return errors;
    }

    private IndigoObject setProperties(CSVRecord record, IndigoObject mol, String[] properties) {
        for (String property : properties) {
            mol.setProperty(property, record.get(property));
        }
        return mol;
    }


}