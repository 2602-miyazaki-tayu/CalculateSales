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
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 指定フォルダ内のファイル一覧を取得
		File[] files = new File(args[0]).listFiles();

		// 売上ファイル（8桁.rcd）を格納するリスト
		List<File> rcdFiles = new ArrayList<>();

		// 売上ファイルのみ抽出
		for (int i = 0; i < files.length; i++) {
		    if (files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
		        rcdFiles.add(files[i]);
		    }
		}

		// 各売上ファイルを読み込み、支店別に売上を加算
		for (int i = 0; i < rcdFiles.size(); i++) {
		    BufferedReader br = null;
		    try {
		        br = new BufferedReader(
		                new FileReader(rcdFiles.get(i)));

		        String branchCode;

		        // 1ファイル内の売上情報を順次読み込み
		        while ((branchCode = br.readLine()) != null) {

		            // 売上金額行を取得
		            String salesLine = br.readLine();

		            // 売上金額を数値に変換
		            long fileSale = Long.parseLong(salesLine);

		            // 該当支店の売上に加算
		            branchSales.put(
		                    branchCode,
		                    branchSales.get(branchCode) + fileSale);
		        }
		    } catch (IOException e) {
		        System.out.println(UNKNOWN_ERROR);
		        return;

		    } finally {
		        // ファイルを閉じる
		        if (br != null) {
		            try {
		                br.close();
		            } catch (IOException e) {
		                System.out.println(UNKNOWN_ERROR);
		                return;
		            }
		        }
		    }
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 支店定義ファイルを1行ずつ読み込み
			while ((line = br.readLine()) != null) {

			    // カンマ区切りで支店コードと支店名を取得
			    String[] items = line.split(",");

			    // 支店コードと支店名をMapに格納
			    branchNames.put(items[0], items[1]);

			    // 売上初期値を0で登録
			    branchSales.put(items[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(
			String path,
			String fileName,
			Map<String, String> branchNames,
			Map<String, Long> branchSales) {

		BufferedWriter bw = null;

		try {
			// 出力ファイル（branch.out）を作成
			File file = new File(path, fileName);
			bw = new BufferedWriter(new FileWriter(file));

			// 支店コードごとに出力
			for (String branchCode : branchNames.keySet()) {

				// 売上金額を取得
				Long sales = branchSales.get(branchCode);

				// ファイルに書き込み
				bw.write(
						branchCode + "," +
								branchNames.get(branchCode) + "," +
								sales);
				bw.newLine();
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;

		} finally {
			// ファイルを閉じる
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
