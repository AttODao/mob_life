# プロジェクトコンテキスト

## 用語

- **モーフ視点制御**: 通常モードと本能モードにおける胴体yaw、頭yaw・pitch、
  クライアントカメラ、直接視点入力、自動復元の一連の状態遷移。
- **本能プロキシ同期**: サーバーPlayerと代理Mobの状態転写、代理Mobの進行、結果適用、
  クライアント表示用状態の導出をまとめた同期ライフサイクル。
- **通常ロコモーション制御**: 通常モードのフォーム、入力、視点、歩容、ライフサイクルを
  まとめて所有し、Mixinと通信を外部アダプターに限定するクライアント境界。
- **Mob視覚レンダリング**: フォーム設定とクライアント環境からフレーム値を導出し、
  シェーダーUBOの名前、順序、サイズ、書き込みを所有する描画境界。

## モーフ視点制御の境界

- `gameplay/view/MorphViewControl`が角度制限、追従速度、入力累積、復元判定を所有する。
- 状態遷移は不変な入力と状態から次の状態を返す決定的な処理とし、Minecraft Entityを
  直接変更しない。
- `Normal`、`InstinctClient`、`InstinctServer`の役割別リデューサーを使う。クライアントや
  サーバーのアダプターは、Entityから値を読み取り、遷移結果を適用し、既存payloadを送る。
- 視点適用結果は、現在角だけを更新して描画補間を維持するか、現在角と前フレーム角を同時に
  更新するかを明示する。
- 代理Mobを駆動する本能サーバー角度の正本は代理Mobである。永続的な角度状態を別に複製せず、
  各tickで代理Mobの姿勢から一時状態を作る。
- 給餌中の注視も`InstinctServer`の状態遷移を通す。ネットワークpayloadの形式と責務は変更しない。

## 本能プロキシ同期の境界

- `InstinctController.tick`はPlayerから代理Mobへのミラーリング、ネイティブAI tick、結果の
  サニタイズ、Playerへの適用、表示用状態の導出までを1回の操作として完結させる。
- 呼び出し側へ代理Mobの生の変位や角度を個別に返さず、位置、姿勢、活動、画面揺れ用サンプルを
  まとめた`InstinctSyncState`を返す。
- `InstinctSyncState`は通信形式に依存しない。`InstinctStatePayload`との相互変換は通信アダプターが
  担い、13項目のpayload形式は維持する。
- クライアントは受信した`InstinctSyncState`を1つの同期状態として保持し、位置適用、視点遷移、
  画面揺れへ必要な値だけを渡す。

## 通常ロコモーション制御の境界

- `ClientLocomotionController`は選択フォーム、baby状態、通常・本能モード遷移、Player再束縛、
  テレポート、プロファイル再読込、切断時のリセットを所有する。
- 入力poll直後の後退無効化と乗り物制限、身体向き、移動入力、ジャンプ、着地後cooldown、
  ウマ系charge、落下補正を同じ境界で順に処理する。
- `MorphGaitControl`はウサギとウマ系の複数tickにまたがる歩容状態を1つの決定的なモデルとして
  表す。歩容ごとの浅いクライアントモジュールは作らない。
- MixinはMinecraftの呼び出し位置と値の適用、`ClientLocomotionPackets`は既存payloadへの変換だけを
  担う。`ClientMorphState`はロコモーション操作を中継しない。

## Mob視覚レンダリングの境界

- `MobVisionPolicy`はフォームの`vision`設定と描画距離、空の暗さ、違和感、本能度から、
  1フレーム分の距離視界と本能vignetteを決定的に導出する。
- `DistanceBlur`は8個の`vec4`を宣言順に並べた128 bytes、`InstinctVignette`は1個の`vec4`を
  並べた16 bytesとし、名前、順序、サイズを同じポリシーで定義する。
- `MobVisionRendering`はGPU bufferの書き込み可否確認、必要時の再生成、std140順序での転送を
  所有する。描画順序への注入、post chain取得、`PostPass`内部MapへのアクセスはMixinアダプターに残す。
- 既存の共有shaderとbase・distanceの2 pass構成は維持する。Javaのレイアウト定義と両JSON、
  GLSLの宣言順がずれた場合はテストで検出する。

## 回帰テスト

- `MorphViewControlTest`は通常クライアント、本能クライアント、本能サーバーのイベント列を検証する。
- 境界値では、累積入力0.5度、頭yaw差±75度、pitch±40度、yawの±180度跨ぎ、非有限入力を維持する。
- `InstinctSyncStateTest`は表示用状態のグループ化、導出値のサニタイズ、既存payloadとの往復変換を
  検証する。
- `MorphGaitControlTest`はウサギの着地cooldownとウマ系のcharge・解放・中断を複数tickの列で
  検証する。
- `MobVisionPolicyTest`は視覚フレームの導出値、UBOサイズ、Java・JSON・GLSL間の宣言順を検証する。
