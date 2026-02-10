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
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		//listFilesを使用してfilesという配列に現在のファイルパスを指定、
		File[] files = new File(args[0]).listFiles();

		//先にファイルの情報を格納する List(ArrayList) を宣言。
		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length ; i++) {
			if(files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
		            //売上ファイルの条件に当てはまったものだけ、List(ArrayList) に追加。
				rcdFiles.add(files[i]);
			}
		}
		//System.out.println("rcdFiles件数：" + rcdFiles.size());
		// rcdFiles には「8桁の数字.rcd」という条件を満たした 売上ファイルだけが格納されている。
		// そのため、売上ファイルの数だけ繰り返し処理を行う。
		for (int i = 0; i < rcdFiles.size(); i++) {

		    BufferedReader br = null;

		    try {
		        // rcdFiles から i 番目の売上ファイルを開く
		        br = new BufferedReader(
		                new FileReader(rcdFiles.get(i))
		        );

		        // 1行目：支店コード
		        String branchCode = br.readLine().trim();

		        // 2行目：売上金額
		        String salesLine = br.readLine().trim();

		        // 売上金額を数値に変換
		        long fileSale = Long.parseLong(salesLine);

		        // 売上を加算
		        Long saleAmount = branchSales.get(branchCode) + fileSale;
		        branchSales.put(branchCode, saleAmount);

		    } catch (IOException e) {
		        System.out.println(UNKNOWN_ERROR);
		        return;

		    } finally {
		        // ファイルが開かれていれば必ず閉じる
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

		// ===== テスト表示（支店ごとの売上確認）=====
//		for (String code : branchSales.keySet()) {
//
//		    System.out.println(
//		        "支店コード：" + code +
//		        " 支店名：" + branchNames.get(code) +
//		        " 売上：" + branchSales.get(code)
//		    );
//		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
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
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				//支店名と支店コードをそれぞれカンマで分割して配列に保存する
				//items[0] には支店コード、items[1] には支店名が格納される。
			    String[] items = line.split(",");
			    //それぞれの要素をMAP型変数branchNamesとbranchSalesにputで追加
			    //ただし1-2段階だと金額は0円と記入するためbranchSalesの金額は0Lとする
			    branchNames.put(items[0], items[1]);
			    branchSales.put(items[0], 0L);

				//System.out.println(line);
			}

		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch(IOException e) {
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

	            // 支店名を取得
	            String branchName = branchNames.get(branchCode);

	            // 売上金額を取得（null 防止のため念のため）
	            Long sales = branchSales.get(branchCode);
	            if (sales == null) {
	                sales = 0L;
	            }

	            // 1行分の文字列を作成
	            String line =
	                    branchCode + "," +
	                    branchName + "," +
	                    sales;

	            // ファイルに書き込み
	            bw.write(line);
	            bw.newLine();
	        }

	    } catch (IOException e) {
	        System.out.println(UNKNOWN_ERROR);
	        return false;

	    } finally {
	        // ファイルが開かれていれば閉じる
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
