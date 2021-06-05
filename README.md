# Link+プラグイン

## 説明
astah*プロジェクトのモデルにあるノードのリンク一覧を表示しジャンプするプラグインです。

# ダウンロード
- [ここ](https://github.com/snytng/linkplus/releases/latest)から`linkplus-<version>.jar`をダウンロードして下さい。

# インストール
- ダウンロードしたプラグインファイルをastah*アプリケーションにドラッグドロップするか、Program Files\asta-professionals\pluginsに置いて下さい。

# 機能説明
- テキストフィールドに記入された文字列で検索したリスト化します。
  - 検索結果の"ノード", "属性", "ダイアグラム", "パス"を表示
  - 検索オプションを`前方一致`か`含む`に切り替えられます。
  - 検索対象を`Note`か`Topic`か`Class`か`全て`に切り替えられます。
    - `Note`はダイアグラムのノート
    - `Topic`はマインドマップのノード
    - `Class`はクラス図のクラス
    - `全て`は全てのダイアグラムのノード
- リストをクリックするとモデルを表示します。
  - ノードのセルをクリックするとモデルを開き、ノードを中央に表示します。
  - それ以外のセルをクリックするとモデルを開きます。

## 免責事項
このastah* pluginは無償で自由に利用することができます。
このastah* pluginを利用したことによる、astah*ファイルの破損により生じた生じたあらゆる損害等について一切責任を負いません。

## 変更履歴
- V0.2
    - ソートしたときの対応するPresentation表示ができていなかった不具合を修正
    - 全ての名前付きのIPresentationを検索の対象にするように変更
    - クラスの属性、操作を検索する機能を追加（検索対象が全てのとき）
    - ステレオタイプを検索する機能を追加（検索対象が全てのとき）
    - セルの選択に応じて表示を更新するように変更
    - Shiftを押しながらヘッダをクリックするとソート状態を解除する機能を追加
- V0.1.1
    - Noteだけではなく、INodePresentationを検索の対象にするように変更
- V0.1
    - 初版


以上
