package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalculateSales {

    private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";

    public static void main(String[] args) {

        // コマンドライン引数が1つであることを確認する。
        // 本プログラムは対象ディレクトリを1つ受け取る仕様のため、
        // 1つ以外の場合は処理を継続できない。
        if (args.length != 1) {
            System.out.println(UNKNOWN_ERROR);
            return;
        }

        File dir = new File(args[0]);

        // 指定されたパスが存在し、かつディレクトリであることを確認する。
        // 存在しない、またはファイルだった場合は処理できないため終了する。
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println(UNKNOWN_ERROR);
            return;
        }

        // 支店コードと支店名を保持するMap
        HashMap<String, String> branchNames = new HashMap<>();
        // 支店コードと売上合計を保持するMap
        HashMap<String, Long> branchSales = new HashMap<>();

        // 商品コードと商品名を保持するMap
        HashMap<String, String> commodityNames = new HashMap<>();
        // 商品コードと売上合計を保持するMap
        HashMap<String, Long> commoditySales = new HashMap<>();

        // 支店定義ファイル
        File branchFile = new File(dir, "branch.lst");

        if (!branchFile.exists()) {
            System.out.println("支店定義ファイルが存在しません");
            return;
        }

        // 定義ファイルの読み込み処理を共通メソッドで実行する。
        // コード形式（正規表現）とエラーメッセージを引数で切り替えることで、
        // 支店・商品どちらにも同じ処理を使えるようにしている。
        if (!readDefinitionFile(
                branchFile,
                branchNames,
                branchSales,
                "^[0-9]{3}$",
                "支店定義ファイルのフォーマットが不正です")) {
            return;
        }

        // 商品定義ファイル
        File commodityFile = new File(dir, "commodity.lst");

        if (!commodityFile.exists()) {
            System.out.println("商品定義ファイルが存在しません");
            return;
        }

        if (!readDefinitionFile(
                commodityFile,
                commodityNames,
                commoditySales,
                "^[a-zA-Z0-9]{8}$",
                "商品定義ファイルのフォーマットが不正です")) {
            return;
        }

        File[] files = dir.listFiles();
        List<File> rcdFiles = new ArrayList<>();

        // 売上ファイルは「8桁数字.rcd」の形式のみ対象とする。
        // isFile()でディレクトリを除外し、正規表現でファイル名を判定する。
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile() &&
                files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
                rcdFiles.add(files[i]);
            }
        }

        // 売上ファイルが存在しない場合は処理対象がないため正常終了する。
        if (rcdFiles.size() == 0) {
            return;
        }

        // 各売上ファイルを順番に処理する。
        for (int i = 0; i < rcdFiles.size(); i++) {

            List<String> lines = readFile(rcdFiles.get(i));

            // 売上ファイルは3行構成であることを確認する。
            if (lines == null || lines.size() != 3) {
                System.out.println(rcdFiles.get(i).getName()
                        + "のフォーマットが不正です");
                return;
            }

            String branchCode = lines.get(0);
            String commodityCode = lines.get(1);

            // 支店コードが定義ファイルに存在するか確認する。
            if (!branchSales.containsKey(branchCode)) {
                System.out.println(rcdFiles.get(i).getName()
                        + "の支店コードが不正です");
                return;
            }

            // 商品コードが定義ファイルに存在するか確認する。
            if (!commoditySales.containsKey(commodityCode)) {
                System.out.println(rcdFiles.get(i).getName()
                        + "の商品コードが不正です");
                return;
            }

            // 売上金額が数値のみで構成されていることを確認する。
            if (!lines.get(2).matches("^[0-9]+$")) {
                System.out.println(rcdFiles.get(i).getName()
                        + "のフォーマットが不正です");
                return;
            }

            long sale = Long.parseLong(lines.get(2));

            // 支店売上に加算
            branchSales.put(branchCode,
                    branchSales.get(branchCode) + sale);

            // 商品売上に加算
            commoditySales.put(commodityCode,
                    commoditySales.get(commodityCode) + sale);

            // 合計金額が10桁を超えていないか確認する。
            if (isOverTenDigits(branchSales.get(branchCode)) ||
                isOverTenDigits(commoditySales.get(commodityCode))) {
                System.out.println("合計金額が10桁を超えました");
                return;
            }
        }

        // 支店別売上を出力する。
        writeFile(new File(dir, "branch.out"),
                branchNames, branchSales);

        // 商品別売上を出力する。
        writeFile(new File(dir, "commodity.out"),
                commodityNames, commoditySales);
    }

    // 定義ファイルを読み込み、コードと名称をMapに格納する。
    // 正規表現に一致しないコードや形式不正があった場合はfalseを返す。
    private static boolean readDefinitionFile(
            File file,
            HashMap<String, String> nameMap,
            HashMap<String, Long> salesMap,
            String regex,
            String errorMessage) {

        List<String> lines = readFile(file);

        if (lines == null) {
            return false;
        }

        for (int i = 0; i < lines.size(); i++) {
            String[] items = lines.get(i).split(",");

            if (items.length != 2 ||
                !items[0].matches(regex)) {
                System.out.println(errorMessage);
                return false;
            }

            nameMap.put(items[0], items[1]);
            salesMap.put(items[0], 0L);
        }

        return true;
    }

    // ファイルを1行ずつ読み込み、Listとして返す。
    // IOExceptionが発生した場合はUNKNOWN_ERRORを出力する。
    private static List<String> readFile(File file) {

        List<String> lines = new ArrayList<>();

        try (BufferedReader br =
                new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = br.readLine()) != null) {
                lines.add(line);
            }

        } catch (IOException e) {
            System.out.println(UNKNOWN_ERROR);
            return null;
        }

        return lines;
    }

    // 集計結果を「コード,名称,売上」の形式で出力する。
    private static void writeFile(
            File file,
            HashMap<String, String> names,
            HashMap<String, Long> sales) {

        try (BufferedWriter bw =
                new BufferedWriter(new FileWriter(file))) {

            for (String key : names.keySet()) {
                bw.write(key + "," +
                        names.get(key) + "," +
                        sales.get(key));
                bw.newLine();
            }

        } catch (IOException e) {
            System.out.println(UNKNOWN_ERROR);
        }
    }

    // 合計金額が10桁を超えているか判定する。
    private static boolean isOverTenDigits(long value) {
        return String.valueOf(value).length() > 10;
    }
}
