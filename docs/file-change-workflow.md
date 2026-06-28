# ファイル変更ワークフロー

このドキュメントは、`src/` 配下へ変更を加える作業の手順（パイプライン）を定義する。作業は5段階に分かれ、各段階は専用のサブエージェントが担当し、メインセッションが順に駆動する。

## 絶対原則

- ユーザーの明示的な許可なしにファイルを変更してはならない。
- `src/` への変更はすべて本ドキュメントのパイプライン（5段階）で行う。各段階は専用サブエージェントが担当し、司令塔はメインセッションが務める。
- 手順の省略・順序の入れ替えを禁止する。各段階は前段階の完了を前提とする。

## パイプライン全体像

```
[1] planner        steering / requirements.md / tasklist.md を作成
      ↓
[2] plan-verifier  requirements.md / tasklist.md を検証（読取専用）
      ↓ ──【ゲートA】ユーザーがプランを承認
[3] implementer    src/ を実装（必ず**1ファイル編集するごとに**ユーザーが内容を確認）
      ↓
[4] tester         テストを実行し green を検証（読取専用＋Bash）
      ↓
[5] done-auditor   tasklist / TODO.md の作業漏れを監査（読取専用）
      ↓
   完了報告
```

**差し戻し**

- 段階2が FAIL、またはゲートA でユーザーが修正を求めた → 段階1 へ戻す。
- 段階4が red（テスト失敗）→ 段階3 へ戻す。
- 段階5が FAIL（作業漏れあり）→ 段階3 へ戻す。

**受け渡し**

サブエージェントはコールド起動であり、メインセッションの文脈を引き継がない。作業状態は `.steering/<dir>/` 配下のファイル（requirements.md / tasklist.md）が保持する。メインセッションは各段階のエージェントへ、対象の steering ディレクトリの絶対パスを必ず渡す。

---

## オーケストレーション手順（メインセッションが従う）

メインセッションは次を順に実施する。各エージェントへは steering ディレクトリの絶対パスを必ず渡す。各エージェントの詳細な振る舞いは「段階別の契約」と各エージェント定義（`.claude/agents/`）に従う。

1. **段階1（planner）を dispatch** — 次のプロンプトを渡す。

   > 作業内容: `<作業内容>`。`docs/file-change-workflow.md` の「段階別の契約」「requirements.md テンプレート」「tasklist.md テンプレート」「作業対象の種別ごとの判定早見表」「前提とするアーキテクチャモデル」を読み、`.steering/<YYYYMMDD>-<hhmm>-<作業内容>/` を作成して `requirements.md` と `tasklist.md` を作成せよ。tasklist のチェックボックスはすべて空で作る。出力に steering ディレクトリの絶対パスを含めよ。

   返答から steering の絶対パスを取得し、以降の全段階へ渡す。

2. **段階2（plan-verifier）を dispatch** — steering パスを渡す。FAIL なら指摘を添えて段階1へ戻す。PASS なら次へ。

3. **【ゲートA】プラン承認** — メインは `requirements.md` / `tasklist.md` の要点をユーザーへ提示し、承認を得る。修正要望があれば段階1へ戻す。承認されたら次へ。

4. **段階3（implementer）を dispatch** — steering パスを渡す。フォアグラウンドで実行する。implementer の Edit/Write は許可制とし、各編集をユーザーが確認する（ゲートB）。

5. **段階4（tester）を dispatch** — steering パスを渡す。red なら失敗ログを添えて段階3へ戻す。green なら次へ。

6. **段階5（done-auditor）を dispatch** — steering パスを渡す。FAIL なら漏れを添えて段階3へ戻す。PASS なら完了。

7. **完了報告** — メインは変更ファイル・テスト結果・tasklist の完了状況をユーザーへ報告する。

---

## 段階別の契約

各エージェントはコールド起動のため、入力は steering パスのみを前提とし、必要な情報は steering 配下のファイルと docs から読む。検証・実行系（段階2・4・5）は読取専用でファイルを変更しない。

| 段階 | 担当            | 入力          | 作業                                                                                    | 出力（メインへ返す）                  |
|----|---------------|-------------|---------------------------------------------------------------------------------------|-----------------------------|
| 1  | planner       | 作業内容        | steering 作成、requirements.md / tasklist.md を作成（チェックは全て空）                               | steering の絶対パス＋両ファイルの要点     |
| 2  | plan-verifier | steering パス | テンプレ項目の充足・作業対象が単一レイヤー/単一種別か・判定早見表との整合・tasklist の網羅性を検査（変更なし）                          | PASS / FAIL＋具体的な指摘          |
| 3  | implementer   | steering パス | tasklist を上から実施（実装・Javadoc・正常系/異常系テスト記載・スタブは `// TODO`・TODO.md 追記）。各編集は許可制。担当チェックを埋める | 変更/追加ファイル一覧                 |
| 4  | tester        | steering パス | `./mvnw test`（または対象クラスを `-Dtest` 指定）を実行し green を検証（ファイル変更なし・読取専用）                     | PASS（green）/ FAIL＋失敗テスト名・ログ |
| 5  | done-auditor  | steering パス | tasklist の全チェック充足・要件の達成・スタブ/DomainObject の TODO.md 登録漏れを監査（変更なし）                      | PASS / FAIL＋漏れの箇条書き         |

---

## エージェント一覧（tools 範囲）

各エージェントは `.claude/agents/` に定義する。`tools` は最小権限とする。

| エージェント          | tools                               | 編集権限                |
|-----------------|-------------------------------------|---------------------|
| `planner`       | Read, Write, Bash, Grep, Glob       | `.steering/` 配下のみ作成 |
| `plan-verifier` | Read, Grep, Glob                    | なし（読取専用）            |
| `implementer`   | Read, Edit, Write, Bash, Grep, Glob | `src/` 配下（各編集は許可制）  |
| `tester`        | Bash, Read, Grep                    | なし（テスト実行のみ）         |
| `done-auditor`  | Read, Grep, Glob                    | なし（読取専用）            |

`model` は各エージェント定義時に決める（空欄ならメインを継承）。

---

## 前提とするアーキテクチャモデル（判断の基礎）

本プロジェクトは DDD レイヤードアーキテクチャを採用する。

**外側／内側の定義**: 呼び出し元を「外側」、呼び出し先を「内側」とする。リクエストは外側から内側へ流れる。レイヤーは外側から内側へ次の順に並ぶ。

```
[外側] presentation (API / Controller)
         ↓ 呼び出す（入力 DTO の Command を引数に渡す）
        application (Service)
         ↓ 呼び出す
        domain (DomainObject / ValueObject / Repository)
         ↓ 呼び出す
[内側] infrastructure (mapper / security)
```

presentation から application への呼び出しでは、application 層に定義した入力 DTO（Command クラス。例: `LoginCommand`）を引数に渡す。Command の生成タイミングや `TODO.md` の扱いは「Command（Service の入力 DTO）の扱い」を参照。

**重要な前提**: ソースコードは必ず外側から内側へ向かって作成される。したがって、作業中のレイヤーについて常に次が成り立つ。

- **外側のレイヤーは既に存在する。** そのメソッドのシグネチャ（名前・引数・戻り値）は確定済みであり、作業対象はそれに合わせる。勝手に変更しない。
- **内側のレイヤーはまだ存在しない。** 作業対象から内側を呼ぶ必要がある場合は「スタブ」を新規作成する。

### スタブの定義

スタブとは、コンパイルを通すためだけに作成するクラス／メソッドである。次を厳守する。

- 中身の具体的な処理を実装しない。考えてもいけない。
- クラスに目印として `// TODO` を記載する。
- 実際の実装は後続タスクで行う（本ワークフローで `TODO.md` に登録する）。

### Command（Service の入力 DTO）の扱い

Service の入力は、プリミティブ型を運ぶ入力 DTO（Command クラス。例: `LoginCommand`）として受け取る。Command はスタブとは異なり、次の方針で扱う。

- **生成タイミング**: Service スタブを作成する時点（＝外側の API 作業時）に同時に作成する。外側の Controller が Command を `new` するため、Command が存在しなければコンパイルが通らない。
- **作成と同時に完成**: Command は実処理を持たない record であり、スタブと完成形が一致する。作成した時点で完了とみなし、後続で実装する中身は無い。
- **TODO.md に登録しない**: 遅延実装が存在しないため `TODO.md` には登録しない。`TODO.md` に積むのは Service 本体と、その内側スタブ（Repository / DomainObject / ValueObject）のみとする。
- **プリミティブ型を保持する**: Command は外側と Service の境界を表す DTO のため、プリミティブ型を保持する。プリミティブ → ValueObject の変換は Service の入口で行う（Service 本体でのプリミティブ型の扱いは「ドメインモデルの利用」の項を参照）。

---

## requirements.md テンプレート

段階1（planner）が steering ディレクトリ内に `requirements.md`（パス: `<steering>/requirements.md`）を作成する。以下のテンプレートを複製して各項目を埋める。`(条件: ...)` が付いた項目は、条件に該当する場合のみ記載する。

```markdown
# requirements

## 作業概要
（このタスクで何を実現するかを記述）

## 作業対象レイヤー
（DDD のうち、今回作業する単一のレイヤーを 1 つだけ。例: application(Service)）

## 作業対象の種別
（API / Service / DomainObject / ValueObject / Repository / infrastructure.mapper / infrastructure.security のいずれか 1 つ）

## 使用するテスト・フレームワーク等
- テストフレームワーク:（JUnit か Mockito か）
- Spring MVC アノテーション:（使用するアノテーション）

## 隣接レイヤー
- 1 つ外側のレイヤー:
- 1 つ内側のレイヤー:

## 外側レイヤーとの契約 (条件: 作業対象が API でない場合)
外側は既に存在するため、必ず実在する。このシグネチャに作業対象を合わせる。
- メソッド名:
- 引数:
- 戻り値:

## 作業対象メソッドのシグネチャ
作業概要を達成するための単一メソッド。
- メソッド名:
- 引数:
- 戻り値:

## 内側レイヤーへの契約 (条件: 作業対象が ValueObject でも Repository でもない場合)
内側はまだ存在しないため、スタブとして新規作成する（実装しない・TODO のみ）。
- メソッド名:
- 引数:
- 戻り値:
- 補足（作業対象が API の場合）: ここでの引数は入力 DTO の Command になる。Command はこのステップで作成するが、スタブ（実装しない・TODO のみ）ではなく作成と同時に完成扱いとし、`TODO.md` には登録しない。詳細は「Command（Service の入力 DTO）の扱い」を参照。

## ドメインモデルの利用 (条件: 作業対象が Service / DomainObject / infrastructure.mapper / infrastructure.security のいずれか)
プリミティブ型への依存を禁止する。必ず DomainObject または ValueObject を作成または既存のものを利用する。
ただし、Service の入力 DTO である Command クラスは例外とし、プリミティブ型を保持してよい（外側との境界を表すため）。Command が保持するプリミティブは、Service の入口で ValueObject / DomainObject に変換する。変換せずプリミティブのまま Service 本体やドメイン層へ持ち込むことを禁止する。Command の扱いは「Command（Service の入力 DTO）の扱い」を参照。
作成する DomainObject / ValueObject はスタブとする（実装しない・TODO のみ）。
- 作成する DomainObject / ValueObject 名:
```

---

## tasklist.md テンプレート

段階1（planner）が steering ディレクトリ内に `tasklist.md` を作成する。チェックボックスはすべて段階3（implementer）が上から順に埋める（検証・実行系の段階2 / 4 / 5 は tasklist に書き込まない）。`(条件: ...)` が付いた項目は条件に該当する場合のみ実施する（該当しなければスキップしてよい）。

```markdown
# tasklist

- [ ] (条件: 作業対象が TODO.md に存在しない場合) 作業対象メソッド名を、プロジェクトルートの TODO.md の 作業対象レイヤーの章に、空のチェックボックスで追記する
- [ ] 作業対象を実装する
- [ ] Javadoc を記載する
- [ ] 正常系のテストコードを記載する
- [ ] 異常系のテストコードを記載する
- [ ] (条件: 作業対象が ValueObject でも Repository でもない場合) 内側レイヤーに作成したスタブのメソッド名を、TODO.md の該当する層の章に、空のチェックボックスで追記する
- [ ] (条件: DomainObject / ValueObject を作成した場合) 作成した DomainObject 名を、TODO.md の該当する層の章に、空のチェックボックスで追記する
- [ ] 今回実装した作業対象に対応する TODO.md のチェックボックスを埋める（完了にする）
```

---

## 作業対象の種別ごとの判定早見表

各セルは requirements.md / tasklist.md に記載した条件をそのまま機械的に適用した結果である。

| 作業対象の種別                 | 外側メソッドを記載<br>(条件: API でない) | 内側スタブを作成・登録<br>(条件: VO/Repository でない) | DomainObject/VO を作成<br>(条件: Service/DomainObject/mapper) |
|-------------------------|:--------------------------:|:--------------------------------------:|:--------------------------------------------------------:|
| API (presentation)      |             ─              |                   ✓                    |                            ─                             |
| Service (application)   |             ✓              |                   ✓                    |                            ✓                             |
| DomainObject (domain)   |             ✓              |                   ✓                    |                            ✓                             |
| ValueObject (domain)    |             ✓              |                   ─                    |                            ─                             |
| Repository (domain)     |             ✓              |                   ─                    |                            ─                             |
| infrastructure.mapper   |             ✓              |                   ✓                    |                            ✓                             |
| infrastructure.security |             ✓              |                   ✓                    |                            ✓                             |

## TODO.md との関係（補足）

- 各レイヤーを実装すると、その内側に作ったスタブ（メソッド名・DomainObject 名）が `TODO.md` に空チェックボックスとして積まれる。
- 後続タスクでそのスタブを実装したら、対応するチェックボックスを埋める。
- API は最も外側で呼び出し元が外部のため、自分自身を `TODO.md` に登録してから着手し、完了時に埋める。
- Command（Service の入力 DTO）は作成と同時に完成するため `TODO.md` に登録しない（「Command（Service の入力 DTO）の扱い」を参照）。