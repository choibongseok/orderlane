package orderlane.app

import orderlane.app.idem.sha256
import orderlane.app.payment.PaymentCodec
import orderlane.app.settlement.ProviderFile
import orderlane.domain.EntryKind
import orderlane.domain.Money
import orderlane.domain.PaymentState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PaymentCodecTest {
    @Test fun everyStateSurvivesAColumnRoundTrip() {
        val a = Money(10_000)
        val states = listOf(PaymentState.Requested(a), PaymentState.Unknown(a, 2), PaymentState.Held(a, 5), PaymentState.Declined(a),
            PaymentState.Approved(a), PaymentState.Approved(a, Money(4000)), PaymentState.FullyCancelled(a))
        for (s in states) {
            val back = PaymentCodec.decode(PaymentCodec.encode(s), a.minor, PaymentCodec.cancelled(s), PaymentCodec.inquiries(s))
            assertThat(back).isEqualTo(s)
        }
    }

    @Test fun unknownStateNameIsRejected() {
        assertThatThrownBy { PaymentCodec.decode("REFUNDED", 1, 0, 0) }.isInstanceOf(IllegalStateException::class.java)
    }
}

class ProviderFileTest {
    @Test fun parsesRowsAndSkipsHeaderAndBlankLines() {
        val rows = ProviderFile.parse("txId,kind,ref,amount\ntx-1,SALE,pay-a,4500\n\ntx-2,CANCEL,k-1,500\n")
        assertThat(rows).hasSize(2)
        assertThat(rows[1].kind).isEqualTo(EntryKind.CANCEL)
        assertThat(rows[1].amount).isEqualTo(Money(500))
    }

    @Test fun malformedRowFailsLoudlyInsteadOfBeingSkipped() {
        assertThatThrownBy { ProviderFile.parse("h\ntx-1,SALE,pay-a") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { ProviderFile.parse("h\ntx-1,SALE,pay-a,abc") }.isInstanceOf(NumberFormatException::class.java)
    }
}

class RequestHashTest {
    @Test fun hashIsStableAndSensitiveToContent() {
        assertThat(sha256("a")).isEqualTo(sha256("a")).hasSize(64)
        assertThat(sha256("a")).isNotEqualTo(sha256("b"))
    }
}
