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
		if (args.length != 1) {
		    System.out.println(UNKNOWN_ERROR);
		    return;
		}
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
			//対象がファイルであり、「数字8桁.rcd」なのか判定
			if (files[i].isFile() && files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
		        rcdFiles.add(files[i]);
		    }
		}

		// 売上ファイル名が連番になっているかチェック
		for (int i = 0; i < rcdFiles.size() - 1; i++) {

			// 現在のファイル名の先頭8桁を数値に変換
			int former = Integer.parseInt(
					rcdFiles.get(i).getName().substring(0, 8));

			// 次のファイル名の先頭8桁を数値に変換
			int latter = Integer.parseInt(
					rcdFiles.get(i + 1).getName().substring(0, 8));

			// 連番になっていない場合はエラー
			if (latter - former != 1) {
				System.out.println("売上ファイル名が連番になっていません");
				return;
			}
		}

		// 各売上ファイルを読み込み、支店別に売上を加算
		for (int i = 0; i < rcdFiles.size(); i++) {

			BufferedReader br = null;

			try {
				br = new BufferedReader(
						new FileReader(rcdFiles.get(i)));

				List<String> lines = new ArrayList<>();
				String line;

				// ファイル読み込み
				while ((line = br.readLine()) != null) {
					lines.add(line);
				}

				// フォーマットチェック（2行であること）
				if (lines.size() != 2) {
					System.out.println(
							rcdFiles.get(i).getName()
									+ "のフォーマットが不正です");
					return;
				}

				// 支店コード存在チェック
				if (!branchSales.containsKey(lines.get(0))) {
					System.out.println(
							rcdFiles.get(i).getName()
									+ "の支店コードが不正です");
					return;
				}
				//売上金額が数字か確認
				if (!lines.get(1).matches("^[0-9]+$")) {
				    System.out.println(UNKNOWN_ERROR);
				    return;
				}
				// 売上加算
				long fileSale = Long.parseLong(lines.get(1));

				Long saleAmount = branchSales.get(lines.get(0)) + fileSale;

				//10桁超えチェック（11桁以上）
				if (saleAmount >= 10000000000L) {
					System.out.println("合計金額が10桁を超えました");
					return;
				}

				branchSales.put(lines.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;

			} finally {
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
		//存在チェック
		File file = new File(path, fileName);
		// 支店定義ファイルが存在しない場合
		if (!file.exists()) {
			System.out.println(FILE_NOT_EXIST);
			return false;
		}

		try {
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 支店定義ファイルを1行ずつ読み込み
			while ((line = br.readLine()) != null) {

				// カンマ区切りで分割
				String[] items = line.split(",");

				// 項目数チェック
				if (items.length != 2) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				String branchCode = items[0];
				String branchName = items[1];

				// 支店コードが3桁の数字かチェック
				if (!branchCode.matches("^[0-9]{3}$")) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}

				// Mapへ格納
				branchNames.put(branchCode, branchName);
				branchSales.put(branchCode, 0L);
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

				// ファイルに書き込み
				bw.write(
						branchCode + "," +
								branchNames.get(branchCode) + "," +
								branchSales.get(branchCode));
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
