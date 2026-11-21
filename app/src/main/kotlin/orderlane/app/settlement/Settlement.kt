package orderlane.app.settlement

import orderlane.app.OrderlaneProperties
import orderlane.app.payment.GatewayClient
import orderlane.domain.EntryKind
import orderlane.domain.LedgerEntry
import orderlane.domain.Mismatch
import orderlane.domain.Money
import orderlane.domain.ProviderTxn
import orderlane.domain.Reconciliation
import orderlane.domain.ReconciliationReport
import orderlane.domain.StoreSettlement
import orderlane.domain.Settlement
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

/** 결제사 거래 내역 파일(CSV: txId,kind,ref,amount) 읽기. */
object ProviderFile {
    fun parse(csv: String): List<ProviderTxn> = csv.lineSequence().drop(1).filter { it.isNotBlank() }.map { line ->
        val c = line.split(',')
        require(c.size == 4) { "거래 내역 파일 형식 오류: $line" }
        ProviderTxn(c[0], EntryKind.valueOf(c[1]), c[2], Money(c[3].toLong()))
    }.toList()
}

@Service
class SettlementService(
    private val jdbc: JdbcClient,
    private val tx: TransactionTemplate,
    private val gateway: GatewayClient,
    private val props: OrderlaneProperties,
) {
    private fun ledger(date: LocalDate): List<LedgerEntry> =
        jdbc.sql("select event_id, store_id, kind, ref, amount_minor from ledger_entry where business_date=? order by id").params(java.sql.Date.valueOf(date))
            .query { rs, _ -> LedgerEntry(rs.getString(1), rs.getLong(2).toString(), EntryKind.valueOf(rs.getString(3)), rs.getString(4), Money(rs.getLong(5))) }.list()

    /** 일 정산. 원장을 처음부터 다시 계산해 그 날짜의 결과를 통째로 바꾼다 — 몇 번을 돌려도 같은 결과다. */
    fun settle(date: LocalDate): List<StoreSettlement> {
        val result = Settlement.settle(ledger(date), props.settlement.feeBps)
        tx.executeWithoutResult {
            jdbc.sql("delete from settlement where business_date=?").params(java.sql.Date.valueOf(date)).update()
            for (s in result) {
                jdbc.sql("insert into settlement (business_date, store_id, sales_minor, cancels_minor, fee_minor, net_minor) values (?,?,?,?,?,?)")
                    .params(java.sql.Date.valueOf(date), s.storeId.toLong(), s.sales.minor, s.cancels.minor, s.fee.minor, s.net.minor).update()
            }
        }
        return result
    }

    fun settlements(date: LocalDate): List<Map<String, Any?>> =
        jdbc.sql("select store_id, sales_minor, cancels_minor, fee_minor, net_minor from settlement where business_date=? order by store_id").params(java.sql.Date.valueOf(date))
            .query { rs, _ -> mapOf("storeId" to rs.getLong(1), "salesMinor" to rs.getLong(2), "cancelsMinor" to rs.getLong(3), "feeMinor" to rs.getLong(4), "netMinor" to rs.getLong(5)) }.list()

    /** 결제사 거래 내역 파일과 내부 원장을 대사해 결과를 저장한다. 같은 입력이면 같은 보고서가 나온다. */
    fun reconcile(date: LocalDate): ReconciliationReport {
        val provider = ProviderFile.parse(gateway.settlementFile(date.toString()))
        val report = Reconciliation.reconcile(ledger(date), provider)
        val d = java.sql.Date.valueOf(date)
        tx.executeWithoutResult {
            jdbc.sql("delete from reconciliation_item where business_date=?").params(d).update()
            jdbc.sql("delete from reconciliation_report where business_date=?").params(d).update()
            jdbc.sql("insert into reconciliation_report (business_date, matched, mismatch_count) values (?,?,?)").params(d, report.matched, report.mismatches.size).update()
            for (m in report.mismatches) {
                jdbc.sql("insert into reconciliation_item (business_date, type, kind, ref, ledger_amount, provider_amount) values (?,?,?,?,?,?)")
                    .params(d, m.type.name, m.kind.name, m.ref, m.ledgerAmount?.minor, m.providerAmount?.minor).update()
            }
        }
        return report
    }

    fun toJson(date: LocalDate, r: ReconciliationReport): Map<String, Any?> = mapOf(
        "businessDate" to date.toString(), "matched" to r.matched, "mismatchCount" to r.mismatches.size, "clean" to r.clean,
        "mismatches" to r.mismatches.map(::item),
    )

    private fun item(m: Mismatch) = mapOf("type" to m.type.name, "kind" to m.kind.name, "ref" to m.ref, "ledgerAmount" to m.ledgerAmount?.minor, "providerAmount" to m.providerAmount?.minor)
}
