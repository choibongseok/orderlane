import {
  VOID3gxj6tk5isa35 as VOID,
  Long2qws0ah9gnpki as Long,
  toString1pkumu07cwy4m as toString,
  IllegalArgumentException_init_$Create$1gox8sdqs91jy as IllegalArgumentException_init_$Create$,
  Unit_instance3kz1zqli6gr1r as Unit_instance,
  protoOf180f3jzyo7rfj as protoOf,
  equalsLong28bsrfhwvd686 as equalsLong,
  initMetadataForClassbxx6q50dy2s7 as initMetadataForClass,
  initMetadataForObject1cxne3s9w65el as initMetadataForObject,
  shiftLeft1ck77p6vapyra as shiftLeft,
  Enum3alwj03lh1n41 as Enum,
  getStringHashCode26igk1bx568vk as getStringHashCode,
  ensureNotNull1e947j3ixpazm as ensureNotNull,
  compare2uud5j30pw5xc as compare,
  noWhenBranchMatchedException2a6r7ubxgky5j as noWhenBranchMatchedException,
  fromInt1lka3ktyu79a4 as fromInt,
  add85si75olwt6n as add,
  StringBuildermazzzhj6kkai as StringBuilder,
  charCodeAt1yspne1d8erbm as charCodeAt,
  Char__toInt_impl_vasixd28v1p2u6jau5g as Char__toInt_impl_vasixd,
  toString1h6jjoch8cjt8 as toString_0,
  _Char___init__impl__6a9atxrzocepzq3zgi as _Char___init__impl__6a9atx,
  padStart36w1507hs626a as padStart,
  Char__compareTo_impl_ypi4mb3bl0kdwnivnp6 as Char__compareTo_impl_ypi4mb,
  joinToStringxqcavsxcmh4q as joinToString,
  joinToString1cxrrlmo0chqs as joinToString_0,
  KtList3hktaavzmj137 as KtList,
  isInterface3d6p8outrmvmk as isInterface,
  initMetadataForCompanion1wyw17z38v6ac as initMetadataForCompanion,
  multiply18i3gv3wlmcjg as multiply,
  THROW_CCE2g6jy02ryeudk as THROW_CCE,
  bitwiseXor10gmbxn4rolze as bitwiseXor,
  bitwiseAnd2g7wmsfd45l12 as bitwiseAnd,
  initMetadataForInterface1egvbzx539z91 as initMetadataForInterface,
  hashCodeq5arwsb9dgti as hashCode,
  equals2au1ep9vhcato as equals,
  to2cs3ny02qtbcb as to,
  collectionSizeOrDefault36dulx8yinfqm as collectionSizeOrDefault,
  ArrayList_init_$Create$a978fvbbts3d as ArrayList_init_$Create$,
  getBooleanHashCode1bbj3u6b3v0a7 as getBooleanHashCode,
  ArrayList_init_$Create$2xrifdqz5xirg as ArrayList_init_$Create$_0,
  listOf1jh22dvmctj1r as listOf,
  listOfvhqybd2zx248 as listOf_0,
  numberRangeToNumber25vse2rgp6rs8 as numberRangeToNumber,
  mapCapacity1h45rc3eh9p2l as mapCapacity,
  coerceAtLeast2bkz8m9ik7hep as coerceAtLeast,
  LinkedHashMap_init_$Create$21wpz6bm2n4yn as LinkedHashMap_init_$Create$,
  getValue48kllevslyh6 as getValue,
  toIntOrNull3w2d066r9pvwm as toIntOrNull,
  charArrayOf27f4r3dozbrk1 as charArrayOf,
  split3d3yeauc4rm2n as split,
  contains2el4s70rdq4ld as contains,
  substringBefore3n7kj60w69hju as substringBefore,
  substringAfter1hku067gwr5ve as substringAfter,
} from './kotlin-kotlin-stdlib.mjs';
//region block: imports
var imul = Math.imul;
//endregion
//region block: pre-declaration
initMetadataForClass(RetryPolicy, 'RetryPolicy', RetryPolicy);
initMetadataForObject(Process, 'Process');
initMetadataForObject(SkipDuplicate, 'SkipDuplicate');
initMetadataForClass(Retry, 'Retry');
initMetadataForObject(DeadLetter, 'DeadLetter');
initMetadataForObject(Consumption, 'Consumption');
initMetadataForClass(IdemStatus, 'IdemStatus', VOID, Enum);
initMetadataForClass(IdemRecord, 'IdemRecord');
initMetadataForObject(Proceed, 'Proceed');
initMetadataForClass(Replay, 'Replay');
initMetadataForObject(Mismatch, 'Mismatch');
initMetadataForObject(InProgress, 'InProgress');
initMetadataForObject(Reclaim, 'Reclaim');
initMetadataForObject(Idempotency, 'Idempotency');
initMetadataForClass(StockCell, 'StockCell');
initMetadataForObject(StockRules, 'StockRules');
initMetadataForObject(Json, 'Json');
initMetadataForClass(RawJson, 'RawJson');
initMetadataForCompanion(Companion);
initMetadataForClass(Money, 'Money');
initMetadataForObject(Math2, 'Math2');
function get_resolved() {
  var tmp;
  var tmp_0;
  if (this instanceof Approved) {
    tmp_0 = true;
  } else {
    tmp_0 = this instanceof Declined;
  }
  if (tmp_0) {
    tmp = true;
  } else {
    tmp = this instanceof FullyCancelled;
  }
  return tmp;
}
initMetadataForInterface(PaymentState, 'PaymentState');
initMetadataForClass(Requested, 'Requested', VOID, VOID, [PaymentState]);
initMetadataForClass(Unknown, 'Unknown', VOID, VOID, [PaymentState]);
initMetadataForClass(Held, 'Held', VOID, VOID, [PaymentState]);
initMetadataForClass(Declined, 'Declined', VOID, VOID, [PaymentState]);
initMetadataForClass(Approved, 'Approved', VOID, VOID, [PaymentState]);
initMetadataForClass(FullyCancelled, 'FullyCancelled', VOID, VOID, [PaymentState]);
initMetadataForClass(Outcome, 'Outcome', VOID, Enum);
initMetadataForClass(Source, 'Source', VOID, Enum);
initMetadataForClass(PaymentPolicy, 'PaymentPolicy', PaymentPolicy);
initMetadataForClass(Applied, 'Applied');
initMetadataForClass(Ignored, 'Ignored');
initMetadataForClass(Rejected, 'Rejected');
initMetadataForObject(PaymentMachine, 'PaymentMachine');
initMetadataForCompanion(Companion_0);
initMetadataForClass(OrderLine, 'OrderLine');
initMetadataForObject(Pricing, 'Pricing');
initMetadataForClass(Step, 'Step');
initMetadataForClass(SimResult, 'SimResult');
initMetadataForClass(Recorder, 'Recorder', Recorder);
initMetadataForClass(Buyer, 'Buyer');
initMetadataForObject(Simulation, 'Simulation');
//endregion
function RetryPolicy(maxAttempts, baseDelayMs) {
  maxAttempts = maxAttempts === VOID ? 3 : maxAttempts;
  baseDelayMs = baseDelayMs === VOID ? new Long(200, 0) : baseDelayMs;
  this.u5_1 = maxAttempts;
  this.v5_1 = baseDelayMs;
  // Inline function 'kotlin.require' call
  // Inline function 'kotlin.require' call
  if (!(this.u5_1 >= 1)) {
    var message = 'Failed requirement.';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
}
protoOf(RetryPolicy).toString = function () {
  return 'RetryPolicy(maxAttempts=' + this.u5_1 + ', baseDelayMs=' + this.v5_1.toString() + ')';
};
protoOf(RetryPolicy).hashCode = function () {
  var result = this.u5_1;
  result = imul(result, 31) + this.v5_1.hashCode() | 0;
  return result;
};
protoOf(RetryPolicy).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof RetryPolicy))
    return false;
  if (!(this.u5_1 === other.u5_1))
    return false;
  if (!equalsLong(this.v5_1, other.v5_1))
    return false;
  return true;
};
function Process() {
}
protoOf(Process).toString = function () {
  return 'Process';
};
protoOf(Process).hashCode = function () {
  return -1688346301;
};
protoOf(Process).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Process))
    return false;
  return true;
};
var Process_instance;
function Process_getInstance() {
  return Process_instance;
}
function SkipDuplicate() {
}
protoOf(SkipDuplicate).toString = function () {
  return 'SkipDuplicate';
};
protoOf(SkipDuplicate).hashCode = function () {
  return 1719507488;
};
protoOf(SkipDuplicate).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof SkipDuplicate))
    return false;
  return true;
};
var SkipDuplicate_instance;
function SkipDuplicate_getInstance() {
  return SkipDuplicate_instance;
}
function Retry(nextAttempt, delayMs) {
  this.w5_1 = nextAttempt;
  this.x5_1 = delayMs;
}
protoOf(Retry).toString = function () {
  return 'Retry(nextAttempt=' + this.w5_1 + ', delayMs=' + this.x5_1.toString() + ')';
};
protoOf(Retry).hashCode = function () {
  var result = this.w5_1;
  result = imul(result, 31) + this.x5_1.hashCode() | 0;
  return result;
};
protoOf(Retry).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Retry))
    return false;
  if (!(this.w5_1 === other.w5_1))
    return false;
  if (!equalsLong(this.x5_1, other.x5_1))
    return false;
  return true;
};
function DeadLetter() {
}
protoOf(DeadLetter).toString = function () {
  return 'DeadLetter';
};
protoOf(DeadLetter).hashCode = function () {
  return -1842901418;
};
protoOf(DeadLetter).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof DeadLetter))
    return false;
  return true;
};
var DeadLetter_instance;
function DeadLetter_getInstance() {
  return DeadLetter_instance;
}
function Consumption() {
}
protoOf(Consumption).y5 = function (alreadyProcessed) {
  return alreadyProcessed ? SkipDuplicate_instance : Process_instance;
};
protoOf(Consumption).z5 = function (attempt, policy) {
  return attempt >= policy.u5_1 ? DeadLetter_instance : new Retry(attempt + 1 | 0, shiftLeft(policy.v5_1, attempt - 1 | 0));
};
var Consumption_instance;
function Consumption_getInstance() {
  return Consumption_instance;
}
var static_init_called;
function static_init() {
  if (static_init_called)
    return Unit_instance;
  static_init_called = true;
  IdemStatus_IN_PROGRESS_instance = new IdemStatus('IN_PROGRESS', 0);
  IdemStatus_COMPLETED_instance = new IdemStatus('COMPLETED', 1);
}
var IdemStatus_IN_PROGRESS_instance;
var IdemStatus_COMPLETED_instance;
function IdemStatus(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function IdemRecord(requestHash, status, lockedUntilMs, responseStatus, responseBody) {
  responseStatus = responseStatus === VOID ? null : responseStatus;
  responseBody = responseBody === VOID ? null : responseBody;
  this.a6_1 = requestHash;
  this.b6_1 = status;
  this.c6_1 = lockedUntilMs;
  this.d6_1 = responseStatus;
  this.e6_1 = responseBody;
}
protoOf(IdemRecord).toString = function () {
  return 'IdemRecord(requestHash=' + this.a6_1 + ', status=' + this.b6_1.toString() + ', lockedUntilMs=' + this.c6_1.toString() + ', responseStatus=' + this.d6_1 + ', responseBody=' + this.e6_1 + ')';
};
protoOf(IdemRecord).hashCode = function () {
  var result = getStringHashCode(this.a6_1);
  result = imul(result, 31) + this.b6_1.hashCode() | 0;
  result = imul(result, 31) + this.c6_1.hashCode() | 0;
  result = imul(result, 31) + (this.d6_1 == null ? 0 : this.d6_1) | 0;
  result = imul(result, 31) + (this.e6_1 == null ? 0 : getStringHashCode(this.e6_1)) | 0;
  return result;
};
protoOf(IdemRecord).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof IdemRecord))
    return false;
  if (!(this.a6_1 === other.a6_1))
    return false;
  if (!this.b6_1.equals(other.b6_1))
    return false;
  if (!equalsLong(this.c6_1, other.c6_1))
    return false;
  if (!(this.d6_1 == other.d6_1))
    return false;
  if (!(this.e6_1 == other.e6_1))
    return false;
  return true;
};
function Proceed() {
}
protoOf(Proceed).toString = function () {
  return 'Proceed';
};
protoOf(Proceed).hashCode = function () {
  return 1205612277;
};
protoOf(Proceed).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Proceed))
    return false;
  return true;
};
var Proceed_instance;
function Proceed_getInstance() {
  return Proceed_instance;
}
function Replay(status, body) {
  this.f6_1 = status;
  this.g6_1 = body;
}
protoOf(Replay).toString = function () {
  return 'Replay(status=' + this.f6_1 + ', body=' + this.g6_1 + ')';
};
protoOf(Replay).hashCode = function () {
  var result = this.f6_1;
  result = imul(result, 31) + getStringHashCode(this.g6_1) | 0;
  return result;
};
protoOf(Replay).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Replay))
    return false;
  if (!(this.f6_1 === other.f6_1))
    return false;
  if (!(this.g6_1 === other.g6_1))
    return false;
  return true;
};
function Mismatch() {
}
protoOf(Mismatch).toString = function () {
  return 'Mismatch';
};
protoOf(Mismatch).hashCode = function () {
  return -1488140185;
};
protoOf(Mismatch).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Mismatch))
    return false;
  return true;
};
var Mismatch_instance;
function Mismatch_getInstance() {
  return Mismatch_instance;
}
function InProgress() {
}
protoOf(InProgress).toString = function () {
  return 'InProgress';
};
protoOf(InProgress).hashCode = function () {
  return 123089611;
};
protoOf(InProgress).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof InProgress))
    return false;
  return true;
};
var InProgress_instance;
function InProgress_getInstance() {
  return InProgress_instance;
}
function Reclaim() {
}
protoOf(Reclaim).toString = function () {
  return 'Reclaim';
};
protoOf(Reclaim).hashCode = function () {
  return -1697344464;
};
protoOf(Reclaim).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Reclaim))
    return false;
  return true;
};
var Reclaim_instance;
function Reclaim_getInstance() {
  return Reclaim_instance;
}
function Idempotency() {
}
protoOf(Idempotency).h6 = function (existing, requestHash, nowMs) {
  if (existing == null)
    return Proceed_instance;
  if (!(existing.a6_1 === requestHash))
    return Mismatch_instance;
  var tmp;
  switch (existing.b6_1.v_1) {
    case 1:
      tmp = new Replay(ensureNotNull(existing.d6_1), ensureNotNull(existing.e6_1));
      break;
    case 0:
      tmp = compare(nowMs, existing.c6_1) < 0 ? InProgress_instance : Reclaim_instance;
      break;
    default:
      noWhenBranchMatchedException();
      break;
  }
  return tmp;
};
var Idempotency_instance;
function Idempotency_getInstance() {
  return Idempotency_instance;
}
function IdemStatus_IN_PROGRESS_getInstance() {
  static_init();
  return IdemStatus_IN_PROGRESS_instance;
}
function IdemStatus_COMPLETED_getInstance() {
  static_init();
  return IdemStatus_COMPLETED_instance;
}
function StockCell(available, version) {
  version = version === VOID ? new Long(0, 0) : version;
  this.i6_1 = available;
  this.j6_1 = version;
  // Inline function 'kotlin.require' call
  if (!(this.i6_1 >= 0)) {
    var message = '\uC7AC\uACE0\uB294 \uC74C\uC218\uAC00 \uB420 \uC218 \uC5C6\uB2E4';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
}
protoOf(StockCell).toString = function () {
  return 'StockCell(available=' + this.i6_1 + ', version=' + this.j6_1.toString() + ')';
};
protoOf(StockCell).hashCode = function () {
  var result = this.i6_1;
  result = imul(result, 31) + this.j6_1.hashCode() | 0;
  return result;
};
protoOf(StockCell).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof StockCell))
    return false;
  if (!(this.i6_1 === other.i6_1))
    return false;
  if (!equalsLong(this.j6_1, other.j6_1))
    return false;
  return true;
};
function StockRules() {
}
protoOf(StockRules).k6 = function (cell, qty) {
  return qty > 0 && cell.i6_1 >= qty;
};
protoOf(StockRules).l6 = function (cell, qty) {
  // Inline function 'kotlin.require' call
  if (!this.k6(cell, qty)) {
    var message = '\uC7AC\uACE0 \uBD80\uC871';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  var tmp = cell.i6_1 - qty | 0;
  // Inline function 'kotlin.Long.plus' call
  var this_0 = cell.j6_1;
  var tmp$ret$2 = add(this_0, fromInt(1));
  return new StockCell(tmp, tmp$ret$2);
};
var StockRules_instance;
function StockRules_getInstance() {
  return StockRules_instance;
}
function Json$obj$lambda(_destruct__k2r9zo) {
  var k = _destruct__k2r9zo.s5();
  var v = _destruct__k2r9zo.t5();
  return Json_instance.m6(k) + ':' + Json_instance.n6(v);
}
function Json$value$lambda(it) {
  return Json_instance.n6(it);
}
function Json() {
}
protoOf(Json).m6 = function (s) {
  var sb = new StringBuilder('"');
  var inductionVariable = 0;
  var last = s.length;
  while (inductionVariable < last) {
    var c = charCodeAt(s, inductionVariable);
    inductionVariable = inductionVariable + 1 | 0;
    if (c === _Char___init__impl__6a9atx(34))
      sb.e4('\\"');
    else if (c === _Char___init__impl__6a9atx(92))
      sb.e4('\\\\');
    else if (c === _Char___init__impl__6a9atx(10))
      sb.e4('\\n');
    else if (c === _Char___init__impl__6a9atx(13))
      sb.e4('\\r');
    else if (c === _Char___init__impl__6a9atx(9))
      sb.e4('\\t');
    else if (Char__compareTo_impl_ypi4mb(c, _Char___init__impl__6a9atx(32)) < 0) {
      var tmp = sb.e4('\\u');
      // Inline function 'kotlin.code' call
      var tmp$ret$0 = Char__toInt_impl_vasixd(c);
      tmp.e4(padStart(toString_0(tmp$ret$0, 16), 4, _Char___init__impl__6a9atx(48)));
    } else
      sb.f4(c);
  }
  return sb.f4(_Char___init__impl__6a9atx(34)).toString();
};
protoOf(Json).o6 = function (pairs) {
  return joinToString(pairs, ',', '{', '}', VOID, VOID, Json$obj$lambda);
};
protoOf(Json).n6 = function (v) {
  var tmp;
  if (v == null) {
    tmp = 'null';
  } else {
    if (typeof v === 'string') {
      tmp = this.m6(v);
    } else {
      var tmp_0;
      if (typeof v === 'boolean') {
        tmp_0 = true;
      } else {
        var tmp_1;
        if (typeof v === 'number') {
          tmp_1 = true;
        } else {
          tmp_1 = v instanceof Long;
        }
        tmp_0 = tmp_1;
      }
      if (tmp_0) {
        tmp = toString(v);
      } else {
        if (isInterface(v, KtList)) {
          tmp = joinToString_0(v, ',', '[', ']', VOID, VOID, Json$value$lambda);
        } else {
          if (v instanceof RawJson) {
            tmp = v.p6_1;
          } else {
            tmp = this.m6(toString(v));
          }
        }
      }
    }
  }
  return tmp;
};
var Json_instance;
function Json_getInstance() {
  return Json_instance;
}
function RawJson(text) {
  this.p6_1 = text;
}
function Companion() {
  Companion_instance = this;
  this.q6_1 = new Money(new Long(0, 0));
}
var Companion_instance;
function Companion_getInstance() {
  if (Companion_instance == null)
    new Companion();
  return Companion_instance;
}
function Money(minor) {
  Companion_getInstance();
  this.r6_1 = minor;
}
protoOf(Money).s6 = function (o) {
  return new Money(Math2_instance.t6(this.r6_1, o.r6_1));
};
protoOf(Money).u6 = function (n) {
  // Inline function 'kotlin.Long.times' call
  var this_0 = this.r6_1;
  var tmp$ret$0 = multiply(this_0, fromInt(n));
  return new Money(tmp$ret$0);
};
protoOf(Money).v6 = function (other) {
  return this.r6_1.e1(other.r6_1);
};
protoOf(Money).x = function (other) {
  return this.v6(other instanceof Money ? other : THROW_CCE());
};
protoOf(Money).toString = function () {
  return this.r6_1.toString();
};
protoOf(Money).hashCode = function () {
  return this.r6_1.hashCode();
};
protoOf(Money).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Money))
    return false;
  if (!equalsLong(this.r6_1, other.r6_1))
    return false;
  return true;
};
function Math2() {
}
protoOf(Math2).t6 = function (a, b) {
  var r = add(a, b);
  // Inline function 'kotlin.require' call
  if (!(compare(bitwiseAnd(bitwiseXor(a, r), bitwiseXor(b, r)), new Long(0, 0)) >= 0)) {
    var message = '\uAE08\uC561 \uC624\uBC84\uD50C\uB85C';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  return r;
};
var Math2_instance;
function Math2_getInstance() {
  return Math2_instance;
}
function Requested(amount) {
  this.w6_1 = amount;
}
protoOf(Requested).x6 = function () {
  return this.w6_1;
};
protoOf(Requested).y6 = function () {
  return 'REQUESTED';
};
protoOf(Requested).toString = function () {
  return 'Requested(amount=' + this.w6_1.toString() + ')';
};
protoOf(Requested).hashCode = function () {
  return this.w6_1.hashCode();
};
protoOf(Requested).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Requested))
    return false;
  if (!this.w6_1.equals(other.w6_1))
    return false;
  return true;
};
function Unknown(amount, inquiries) {
  this.a7_1 = amount;
  this.b7_1 = inquiries;
}
protoOf(Unknown).x6 = function () {
  return this.a7_1;
};
protoOf(Unknown).y6 = function () {
  return 'UNKNOWN';
};
protoOf(Unknown).toString = function () {
  return 'Unknown(amount=' + this.a7_1.toString() + ', inquiries=' + this.b7_1 + ')';
};
protoOf(Unknown).hashCode = function () {
  var result = this.a7_1.hashCode();
  result = imul(result, 31) + this.b7_1 | 0;
  return result;
};
protoOf(Unknown).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Unknown))
    return false;
  if (!this.a7_1.equals(other.a7_1))
    return false;
  if (!(this.b7_1 === other.b7_1))
    return false;
  return true;
};
function Held(amount, inquiries) {
  this.c7_1 = amount;
  this.d7_1 = inquiries;
}
protoOf(Held).x6 = function () {
  return this.c7_1;
};
protoOf(Held).y6 = function () {
  return 'HELD';
};
protoOf(Held).toString = function () {
  return 'Held(amount=' + this.c7_1.toString() + ', inquiries=' + this.d7_1 + ')';
};
protoOf(Held).hashCode = function () {
  var result = this.c7_1.hashCode();
  result = imul(result, 31) + this.d7_1 | 0;
  return result;
};
protoOf(Held).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Held))
    return false;
  if (!this.c7_1.equals(other.c7_1))
    return false;
  if (!(this.d7_1 === other.d7_1))
    return false;
  return true;
};
function Declined(amount) {
  this.e7_1 = amount;
}
protoOf(Declined).x6 = function () {
  return this.e7_1;
};
protoOf(Declined).y6 = function () {
  return 'DECLINED';
};
protoOf(Declined).toString = function () {
  return 'Declined(amount=' + this.e7_1.toString() + ')';
};
protoOf(Declined).hashCode = function () {
  return this.e7_1.hashCode();
};
protoOf(Declined).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Declined))
    return false;
  if (!this.e7_1.equals(other.e7_1))
    return false;
  return true;
};
function Approved(amount, cancelled) {
  cancelled = cancelled === VOID ? Companion_getInstance().q6_1 : cancelled;
  this.f7_1 = amount;
  this.g7_1 = cancelled;
  var containsUpper = this.f7_1.r6_1;
  var containsArg = this.g7_1.r6_1;
  // Inline function 'kotlin.require' call
  if (!(compare(new Long(0, 0), containsArg) <= 0 ? compare(containsArg, containsUpper) < 0 : false)) {
    var message = '\uBD80\uBD84 \uCDE8\uC18C \uAE08\uC561\uC774 \uBC94\uC704\uB97C \uBC97\uC5B4\uB0A8';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
}
protoOf(Approved).x6 = function () {
  return this.f7_1;
};
protoOf(Approved).y6 = function () {
  return equalsLong(this.g7_1.r6_1, new Long(0, 0)) ? 'APPROVED' : 'PARTIALLY_CANCELLED';
};
protoOf(Approved).toString = function () {
  return 'Approved(amount=' + this.f7_1.toString() + ', cancelled=' + this.g7_1.toString() + ')';
};
protoOf(Approved).hashCode = function () {
  var result = this.f7_1.hashCode();
  result = imul(result, 31) + this.g7_1.hashCode() | 0;
  return result;
};
protoOf(Approved).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Approved))
    return false;
  if (!this.f7_1.equals(other.f7_1))
    return false;
  if (!this.g7_1.equals(other.g7_1))
    return false;
  return true;
};
function FullyCancelled() {
}
function PaymentState() {
}
var static_init_called_0;
function static_init_0() {
  if (static_init_called_0)
    return Unit_instance;
  static_init_called_0 = true;
  Outcome_APPROVED_instance = new Outcome('APPROVED', 0);
  Outcome_DECLINED_instance = new Outcome('DECLINED', 1);
  Outcome_PENDING_instance = new Outcome('PENDING', 2);
  Outcome_NOT_FOUND_instance = new Outcome('NOT_FOUND', 3);
  Outcome_TIMEOUT_instance = new Outcome('TIMEOUT', 4);
}
var Outcome_APPROVED_instance;
var Outcome_DECLINED_instance;
var Outcome_PENDING_instance;
var Outcome_NOT_FOUND_instance;
var Outcome_TIMEOUT_instance;
function Outcome(name, ordinal) {
  Enum.call(this, name, ordinal);
}
var static_init_called_1;
function static_init_1() {
  if (static_init_called_1)
    return Unit_instance;
  static_init_called_1 = true;
  Source_REQUEST_instance = new Source('REQUEST', 0);
  Source_INQUIRY_instance = new Source('INQUIRY', 1);
  Source_CALLBACK_instance = new Source('CALLBACK', 2);
}
var Source_REQUEST_instance;
var Source_INQUIRY_instance;
var Source_CALLBACK_instance;
function Source(name, ordinal) {
  Enum.call(this, name, ordinal);
}
function PaymentPolicy(maxInquiries) {
  maxInquiries = maxInquiries === VOID ? 3 : maxInquiries;
  this.h7_1 = maxInquiries;
}
protoOf(PaymentPolicy).toString = function () {
  return 'PaymentPolicy(maxInquiries=' + this.h7_1 + ')';
};
protoOf(PaymentPolicy).hashCode = function () {
  return this.h7_1;
};
protoOf(PaymentPolicy).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof PaymentPolicy))
    return false;
  if (!(this.h7_1 === other.h7_1))
    return false;
  return true;
};
function Applied(state) {
  this.i7_1 = state;
}
protoOf(Applied).j7 = function () {
  return this.i7_1;
};
protoOf(Applied).toString = function () {
  return 'Applied(state=' + toString(this.i7_1) + ')';
};
protoOf(Applied).hashCode = function () {
  return hashCode(this.i7_1);
};
protoOf(Applied).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Applied))
    return false;
  if (!equals(this.i7_1, other.i7_1))
    return false;
  return true;
};
function Ignored(state, reason) {
  this.k7_1 = state;
  this.l7_1 = reason;
}
protoOf(Ignored).j7 = function () {
  return this.k7_1;
};
protoOf(Ignored).toString = function () {
  return 'Ignored(state=' + toString(this.k7_1) + ', reason=' + this.l7_1 + ')';
};
protoOf(Ignored).hashCode = function () {
  var result = hashCode(this.k7_1);
  result = imul(result, 31) + getStringHashCode(this.l7_1) | 0;
  return result;
};
protoOf(Ignored).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Ignored))
    return false;
  if (!equals(this.k7_1, other.k7_1))
    return false;
  if (!(this.l7_1 === other.l7_1))
    return false;
  return true;
};
function Rejected(state, code, message) {
  this.m7_1 = state;
  this.n7_1 = code;
  this.o7_1 = message;
}
protoOf(Rejected).j7 = function () {
  return this.m7_1;
};
protoOf(Rejected).toString = function () {
  return 'Rejected(state=' + toString(this.m7_1) + ', code=' + this.n7_1 + ', message=' + this.o7_1 + ')';
};
protoOf(Rejected).hashCode = function () {
  var result = hashCode(this.m7_1);
  result = imul(result, 31) + getStringHashCode(this.n7_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.o7_1) | 0;
  return result;
};
protoOf(Rejected).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Rejected))
    return false;
  if (!equals(this.m7_1, other.m7_1))
    return false;
  if (!(this.n7_1 === other.n7_1))
    return false;
  if (!(this.o7_1 === other.o7_1))
    return false;
  return true;
};
function unresolved($this, state, inquiries, outcome, source, policy, pendingState) {
  var amount = state.x6();
  var tmp;
  if (outcome.equals(Outcome_APPROVED_getInstance())) {
    tmp = new Applied(new Approved(amount));
  } else if (outcome.equals(Outcome_DECLINED_getInstance()) || (outcome.equals(Outcome_NOT_FOUND_getInstance()) && source.equals(Source_INQUIRY_getInstance()))) {
    tmp = new Applied(new Declined(amount));
  } else if (outcome.equals(Outcome_PENDING_getInstance()) && source.equals(Source_INQUIRY_getInstance())) {
    var n = inquiries + 1 | 0;
    var tmp_0;
    var tmp_1;
    if (state instanceof Held) {
      tmp_1 = true;
    } else {
      tmp_1 = n >= policy.h7_1;
    }
    if (tmp_1) {
      tmp_0 = new Applied(new Held(amount, n));
    } else {
      tmp_0 = new Applied(pendingState(n));
    }
    tmp = tmp_0;
  } else if (outcome.equals(Outcome_PENDING_getInstance())) {
    tmp = new Ignored(state, 'stale-pending');
  } else if (outcome.equals(Outcome_TIMEOUT_getInstance()) && source.equals(Source_INQUIRY_getInstance())) {
    var tmp_2;
    var tmp_3;
    if (state instanceof Held) {
      tmp_3 = true;
    } else {
      tmp_3 = (inquiries + 1 | 0) >= policy.h7_1;
    }
    if (tmp_3) {
      tmp_2 = new Held(amount, inquiries + 1 | 0);
    } else {
      tmp_2 = pendingState(inquiries + 1 | 0);
    }
    tmp = new Applied(tmp_2);
  } else if (outcome.equals(Outcome_TIMEOUT_getInstance())) {
    tmp = new Ignored(state, 'duplicate-timeout');
  } else {
    tmp = notApplicable($this, state, outcome, source);
  }
  return tmp;
}
function notApplicable($this, state, outcome, source) {
  return new Rejected(state, 'not-applicable', state.y6() + ' \uC0C1\uD0DC\uC5D0\uC11C ' + source.toString() + '/' + outcome.toString() + ' \uB294 \uD5C8\uC6A9\uB418\uC9C0 \uC54A\uC74C');
}
function PaymentMachine$on$lambda($amount) {
  return function (n) {
    return new Unknown($amount, n);
  };
}
function PaymentMachine$on$lambda_0($amount) {
  return function (n) {
    return new Held($amount, n);
  };
}
function PaymentMachine() {
}
protoOf(PaymentMachine).p7 = function (amount) {
  // Inline function 'kotlin.require' call
  if (!(compare(amount.r6_1, new Long(0, 0)) > 0)) {
    var message = '\uACB0\uC81C \uAE08\uC561\uC740 0 \uBCF4\uB2E4 \uCEE4\uC57C \uD568';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  return new Requested(amount);
};
protoOf(PaymentMachine).q7 = function (state, outcome, source, policy) {
  var amount = state.x6();
  var tmp;
  if (state instanceof Requested) {
    tmp = outcome.equals(Outcome_PENDING_getInstance()) ? new Ignored(state, 'stale-pending') : outcome.equals(Outcome_APPROVED_getInstance()) && !source.equals(Source_INQUIRY_getInstance()) ? new Applied(new Approved(amount)) : outcome.equals(Outcome_DECLINED_getInstance()) && !source.equals(Source_INQUIRY_getInstance()) ? new Applied(new Declined(amount)) : outcome.equals(Outcome_TIMEOUT_getInstance()) && source.equals(Source_REQUEST_getInstance()) ? new Applied(new Unknown(amount, 0)) : notApplicable(this, state, outcome, source);
  } else {
    if (state instanceof Unknown) {
      tmp = unresolved(this, state, state.b7_1, outcome, source, policy, PaymentMachine$on$lambda(amount));
    } else {
      if (state instanceof Held) {
        tmp = unresolved(this, state, state.d7_1, outcome, source, policy, PaymentMachine$on$lambda_0(amount));
      } else {
        var tmp_0;
        if (state instanceof Approved) {
          tmp_0 = true;
        } else {
          tmp_0 = state instanceof FullyCancelled;
        }
        if (tmp_0) {
          switch (outcome.v_1) {
            case 0:
              tmp = new Ignored(state, 'duplicate-approved');
              break;
            case 2:
              tmp = new Ignored(state, 'stale-pending');
              break;
            case 1:
              tmp = new Rejected(state, 'conflict', state.y6() + ' \uACB0\uC81C\uC5D0 \uAC70\uC808 \uD1B5\uC9C0\uAC00 \uC654\uB2E4');
              break;
            default:
              tmp = notApplicable(this, state, outcome, source);
              break;
          }
        } else {
          if (state instanceof Declined) {
            switch (outcome.v_1) {
              case 1:
              case 3:
                tmp = new Ignored(state, 'duplicate-declined');
                break;
              case 2:
                tmp = new Ignored(state, 'stale-pending');
                break;
              case 0:
                tmp = new Rejected(state, 'conflict', '\uAC70\uC808 \uD655\uC815\uB41C \uACB0\uC81C\uC5D0 \uC2B9\uC778 \uD1B5\uC9C0\uAC00 \uC654\uB2E4');
                break;
              default:
                tmp = notApplicable(this, state, outcome, source);
                break;
            }
          } else {
            noWhenBranchMatchedException();
          }
        }
      }
    }
  }
  return tmp;
};
protoOf(PaymentMachine).r7 = function (state, outcome, source, policy, $super) {
  policy = policy === VOID ? new PaymentPolicy() : policy;
  return $super === VOID ? this.q7(state, outcome, source, policy) : $super.q7.call(this, state, outcome, source, policy);
};
var PaymentMachine_instance;
function PaymentMachine_getInstance() {
  return PaymentMachine_instance;
}
function Outcome_APPROVED_getInstance() {
  static_init_0();
  return Outcome_APPROVED_instance;
}
function Outcome_DECLINED_getInstance() {
  static_init_0();
  return Outcome_DECLINED_instance;
}
function Outcome_PENDING_getInstance() {
  static_init_0();
  return Outcome_PENDING_instance;
}
function Outcome_NOT_FOUND_getInstance() {
  static_init_0();
  return Outcome_NOT_FOUND_instance;
}
function Outcome_TIMEOUT_getInstance() {
  static_init_0();
  return Outcome_TIMEOUT_instance;
}
function Source_REQUEST_getInstance() {
  static_init_1();
  return Source_REQUEST_instance;
}
function Source_INQUIRY_getInstance() {
  static_init_1();
  return Source_INQUIRY_instance;
}
function Source_CALLBACK_getInstance() {
  static_init_1();
  return Source_CALLBACK_instance;
}
function Companion_0() {
  this.s7_1 = 20;
}
var Companion_instance_0;
function Companion_getInstance_0() {
  return Companion_instance_0;
}
function OrderLine(menuItemId, quantity, unitPrice, optionExtra) {
  optionExtra = optionExtra === VOID ? Companion_getInstance().q6_1 : optionExtra;
  this.t7_1 = menuItemId;
  this.u7_1 = quantity;
  this.v7_1 = unitPrice;
  this.w7_1 = optionExtra;
  var containsArg = this.u7_1;
  // Inline function 'kotlin.require' call
  if (!(1 <= containsArg ? containsArg <= 20 : false)) {
    var message = '\uC218\uB7C9\uC740 1..20';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.require' call
  if (!(compare(this.v7_1.r6_1, new Long(0, 0)) >= 0 && compare(this.w7_1.r6_1, new Long(0, 0)) >= 0)) {
    var message_0 = '\uAE08\uC561\uC740 \uC74C\uC218\uC77C \uC218 \uC5C6\uC74C';
    throw IllegalArgumentException_init_$Create$(toString(message_0));
  }
}
protoOf(OrderLine).x7 = function () {
  return this.v7_1.s6(this.w7_1).u6(this.u7_1);
};
protoOf(OrderLine).toString = function () {
  return 'OrderLine(menuItemId=' + this.t7_1.toString() + ', quantity=' + this.u7_1 + ', unitPrice=' + this.v7_1.toString() + ', optionExtra=' + this.w7_1.toString() + ')';
};
protoOf(OrderLine).hashCode = function () {
  var result = this.t7_1.hashCode();
  result = imul(result, 31) + this.u7_1 | 0;
  result = imul(result, 31) + this.v7_1.hashCode() | 0;
  result = imul(result, 31) + this.w7_1.hashCode() | 0;
  return result;
};
protoOf(OrderLine).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof OrderLine))
    return false;
  if (!equalsLong(this.t7_1, other.t7_1))
    return false;
  if (!(this.u7_1 === other.u7_1))
    return false;
  if (!this.v7_1.equals(other.v7_1))
    return false;
  if (!this.w7_1.equals(other.w7_1))
    return false;
  return true;
};
function Pricing() {
}
protoOf(Pricing).y7 = function (lines) {
  // Inline function 'kotlin.collections.isNotEmpty' call
  // Inline function 'kotlin.require' call
  if (!!lines.l()) {
    var message = '\uC8FC\uBB38 \uC904\uC774 \uBE44\uC5B4 \uC788\uC74C';
    throw IllegalArgumentException_init_$Create$(toString(message));
  }
  // Inline function 'kotlin.collections.fold' call
  var accumulator = Companion_getInstance().q6_1;
  var _iterator__ex2g4s = lines.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    accumulator = accumulator.s6(element.x7());
  }
  return accumulator;
};
var Pricing_instance;
function Pricing_getInstance() {
  return Pricing_instance;
}
function Step(atMs, actor, action, detail, tone, rule, state) {
  rule = rule === VOID ? null : rule;
  state = state === VOID ? null : state;
  this.z7_1 = atMs;
  this.a8_1 = actor;
  this.b8_1 = action;
  this.c8_1 = detail;
  this.d8_1 = tone;
  this.e8_1 = rule;
  this.f8_1 = state;
}
protoOf(Step).toString = function () {
  return 'Step(atMs=' + this.z7_1.toString() + ', actor=' + this.a8_1 + ', action=' + this.b8_1 + ', detail=' + this.c8_1 + ', tone=' + this.d8_1 + ', rule=' + this.e8_1 + ', state=' + this.f8_1 + ')';
};
protoOf(Step).hashCode = function () {
  var result = this.z7_1.hashCode();
  result = imul(result, 31) + getStringHashCode(this.a8_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.b8_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.c8_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.d8_1) | 0;
  result = imul(result, 31) + (this.e8_1 == null ? 0 : getStringHashCode(this.e8_1)) | 0;
  result = imul(result, 31) + (this.f8_1 == null ? 0 : getStringHashCode(this.f8_1)) | 0;
  return result;
};
protoOf(Step).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Step))
    return false;
  if (!equalsLong(this.z7_1, other.z7_1))
    return false;
  if (!(this.a8_1 === other.a8_1))
    return false;
  if (!(this.b8_1 === other.b8_1))
    return false;
  if (!(this.c8_1 === other.c8_1))
    return false;
  if (!(this.d8_1 === other.d8_1))
    return false;
  if (!(this.e8_1 == other.e8_1))
    return false;
  if (!(this.f8_1 == other.f8_1))
    return false;
  return true;
};
function SimResult(scenario, variant, title, steps, facts, verdictOk, verdict) {
  this.g8_1 = scenario;
  this.h8_1 = variant;
  this.i8_1 = title;
  this.j8_1 = steps;
  this.k8_1 = facts;
  this.l8_1 = verdictOk;
  this.m8_1 = verdict;
}
protoOf(SimResult).n8 = function () {
  var tmp = Json_instance;
  var tmp_0 = to('scenario', this.g8_1);
  var tmp_1 = to('variant', this.h8_1);
  var tmp_2 = to('title', this.i8_1);
  // Inline function 'kotlin.collections.map' call
  var this_0 = this.j8_1;
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
  var _iterator__ex2g4s = this_0.e();
  while (_iterator__ex2g4s.f()) {
    var item = _iterator__ex2g4s.g();
    var tmp$ret$2 = new RawJson(Json_instance.o6([to('atMs', item.z7_1), to('actor', item.a8_1), to('action', item.b8_1), to('detail', item.c8_1), to('tone', item.d8_1), to('rule', item.e8_1), to('state', item.f8_1)]));
    destination.h1(tmp$ret$2);
  }
  var tmp_3 = to('steps', destination);
  // Inline function 'kotlin.collections.map' call
  var this_1 = this.k8_1;
  // Inline function 'kotlin.collections.mapTo' call
  var destination_0 = ArrayList_init_$Create$(collectionSizeOrDefault(this_1, 10));
  var _iterator__ex2g4s_0 = this_1.e();
  while (_iterator__ex2g4s_0.f()) {
    var item_0 = _iterator__ex2g4s_0.g();
    var tmp$ret$5 = new RawJson(Json_instance.o6([to('k', item_0.q5_1), to('v', item_0.r5_1)]));
    destination_0.h1(tmp$ret$5);
  }
  return tmp.o6([tmp_0, tmp_1, tmp_2, tmp_3, to('facts', destination_0), to('verdictOk', this.l8_1), to('verdict', this.m8_1)]);
};
protoOf(SimResult).toString = function () {
  return 'SimResult(scenario=' + this.g8_1 + ', variant=' + this.h8_1 + ', title=' + this.i8_1 + ', steps=' + toString(this.j8_1) + ', facts=' + toString(this.k8_1) + ', verdictOk=' + this.l8_1 + ', verdict=' + this.m8_1 + ')';
};
protoOf(SimResult).hashCode = function () {
  var result = getStringHashCode(this.g8_1);
  result = imul(result, 31) + getStringHashCode(this.h8_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.i8_1) | 0;
  result = imul(result, 31) + hashCode(this.j8_1) | 0;
  result = imul(result, 31) + hashCode(this.k8_1) | 0;
  result = imul(result, 31) + getBooleanHashCode(this.l8_1) | 0;
  result = imul(result, 31) + getStringHashCode(this.m8_1) | 0;
  return result;
};
protoOf(SimResult).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof SimResult))
    return false;
  if (!(this.g8_1 === other.g8_1))
    return false;
  if (!(this.h8_1 === other.h8_1))
    return false;
  if (!(this.i8_1 === other.i8_1))
    return false;
  if (!equals(this.j8_1, other.j8_1))
    return false;
  if (!equals(this.k8_1, other.k8_1))
    return false;
  if (!(this.l8_1 === other.l8_1))
    return false;
  if (!(this.m8_1 === other.m8_1))
    return false;
  return true;
};
function Recorder() {
  var tmp = this;
  // Inline function 'kotlin.collections.mutableListOf' call
  tmp.o8_1 = ArrayList_init_$Create$_0();
  this.p8_1 = new Long(0, 0);
}
protoOf(Recorder).q8 = function (actor, action, detail, tone, rule, state) {
  var tmp0 = this.o8_1;
  // Inline function 'kotlin.collections.plusAssign' call
  var element = new Step(this.p8_1, actor, action, detail, tone, rule, state);
  tmp0.h1(element);
};
protoOf(Recorder).r8 = function (actor, action, detail, tone, rule, state, $super) {
  tone = tone === VOID ? 'info' : tone;
  rule = rule === VOID ? null : rule;
  state = state === VOID ? null : state;
  var tmp;
  if ($super === VOID) {
    this.q8(actor, action, detail, tone, rule, state);
    tmp = Unit_instance;
  } else {
    tmp = $super.q8.call(this, actor, action, detail, tone, rule, state);
  }
  return tmp;
};
protoOf(Recorder).s8 = function (ms) {
  this.p8_1 = add(this.p8_1, ms);
};
protoOf(Recorder).t8 = function (ms, $super) {
  ms = ms === VOID ? new Long(10, 0) : ms;
  var tmp;
  if ($super === VOID) {
    this.s8(ms);
    tmp = Unit_instance;
  } else {
    tmp = $super.s8.call(this, ms);
  }
  return tmp;
};
function happy($this) {
  var r = new Recorder();
  var total = Pricing_instance.y7($this.v8_1);
  r.r8('client', 'POST /orders', 'Idempotency-Key: k-1001, \uC544\uBA54\uB9AC\uCE74\uB178 2\uC794(\uC635\uC158 +500)', 'info');
  r.t8();
  r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '\uCC98\uC74C \uBCF4\uB294 \uD0A4\uB77C \uC774 \uC694\uCCAD\uC774 \uCC98\uB9AC\uD55C\uB2E4', 'ok', 'Idempotency.decide(null, hash, now) = ' + toString(Idempotency_instance.h6(null, 'h', new Long(0, 0))));
  r.t8();
  var stock = StockRules_instance.l6(new StockCell(10), 2);
  r.r8('inventory', '\uC7AC\uACE0 \uC608\uC57D', '\uC7AC\uACE0 10 \u2192 ' + stock.i6_1, 'ok', 'StockRules.reserve(StockCell(10), 2)');
  r.r8('api', '\uAE08\uC561 \uACC4\uC0B0', '(4500+500)\xD72 = ' + total.r6_1.toString() + '\uC6D0', 'info', 'Pricing.total(lines)');
  var pay = PaymentMachine_instance.p7(total);
  r.q8('payment', '\uC2B9\uC778 \uC694\uCCAD', '\uC0C1\uD0DC ' + pay.y6(), 'info', 'PaymentMachine.initial(' + total.toString() + ')', pay.y6());
  r.s8(new Long(120, 0));
  r.r8('gateway', '\uC2B9\uC778', '\uC2B9\uC778 \uC751\uB2F5', 'ok');
  var tmp = PaymentMachine_instance.r7(pay, Outcome_APPROVED_getInstance(), Source_REQUEST_getInstance());
  pay = (tmp instanceof Applied ? tmp : THROW_CCE()).i7_1;
  r.q8('payment', '\uC804\uC774', 'REQUESTED \u2192 ' + pay.y6(), 'ok', 'PaymentMachine.on(REQUESTED, APPROVED, REQUEST)', pay.y6());
  r.r8('outbox', '\uAC19\uC740 \uD2B8\uB79C\uC7AD\uC158\uC5D0 \uAE30\uB85D', 'ORDER_PLACED, PAYMENT_APPROVED', 'info');
  r.s8(new Long(60, 0));
  r.r8('relay', '\uBE0C\uB85C\uCEE4 \uBC1C\uD589', '\uC544\uC6C3\uBC15\uC2A4 2\uAC74 \uBC1C\uD589(\uC801\uC5B4\uB3C4 \uD55C \uBC88)', 'info');
  var _iterator__ex2g4s = listOf(['kitchen', 'notification', 'ledger']).e();
  while (_iterator__ex2g4s.f()) {
    var c = _iterator__ex2g4s.g();
    r.r8('consumer:' + c, '\uC18C\uBE44', '\uCC98\uC74C \uBCF8 \uC774\uBCA4\uD2B8\uB77C \uCC98\uB9AC', 'ok', 'Consumption.onArrival(false) = ' + toString(Consumption_instance.y5(false)));
  }
  r.r8('api', '\uC751\uB2F5', '201 Created, \uC8FC\uBB38 PAID', 'ok');
  return new SimResult('happy', 'default', '\uC8FC\uBB38 \uD55C \uAC74\uC758 \uC815\uC0C1 \uD750\uB984', r.o8_1, listOf([to('\uACB0\uC81C \uAE08\uC561', total.r6_1.toString() + '\uC6D0'), to('\uACB0\uC81C \uC0C1\uD0DC', pay.y6()), to('\uC7AC\uACE0', '' + stock.i6_1)]), true, '\uC8FC\uBB38 PAID, \uC7AC\uACE0 ' + stock.i6_1);
}
function duplicateRequest($this, variant) {
  var r = new Recorder();
  var hashA = 'sha:body-A';
  var ok = true;
  var verdict = '';
  var title;
  switch (variant) {
    case 'replay':
      title = '\uAC19\uC740 \uD0A4 + \uAC19\uC740 \uBCF8\uBB38: \uAC19\uC740 \uC751\uB2F5';
      break;
    case 'in-flight':
      title = '\uCC98\uB9AC \uC911\uC5D0 \uAC19\uC740 \uC694\uCCAD\uC774 \uB2E4\uC2DC \uC634: \uCDA9\uB3CC \uC751\uB2F5';
      break;
    case 'mismatch':
      title = '\uAC19\uC740 \uD0A4 + \uB2E4\uB978 \uBCF8\uBB38: \uAC70\uBD80';
      break;
    default:
      title = '\uCC98\uB9AC\uD558\uB358 \uC11C\uBC84\uAC00 \uC8FD\uC740 \uB4A4 \uAC19\uC740 \uC694\uCCAD: \uC774\uC5B4\uBC1B\uAE30';
      break;
  }
  r.r8('client', 'POST /orders', 'Idempotency-Key: k-2001, \uBCF8\uBB38 A', 'info');
  r.t8();
  var rec = null;
  var d1 = Idempotency_instance.h6(rec, hashA, r.p8_1);
  r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '\uCC98\uC74C \uBCF4\uB294 \uD0A4: ' + toString(d1), 'ok', 'Idempotency.decide(null, A, ' + r.p8_1.toString() + ')');
  var tmp = IdemStatus_IN_PROGRESS_getInstance();
  // Inline function 'kotlin.Long.plus' call
  var this_0 = r.p8_1;
  var tmp$ret$0 = add(this_0, fromInt(30000));
  rec = new IdemRecord(hashA, tmp, tmp$ret$0);
  r.r8('store', '\uD0A4 \uC120\uC810', 'IN_PROGRESS, \uC7A0\uAE08 30\uCD08', 'info');
  switch (variant) {
    case 'replay':
      r.s8(new Long(150, 0));
      rec = new IdemRecord(hashA, IdemStatus_COMPLETED_getInstance(), new Long(0, 0), 201, '{order:1001,PAID}');
      r.r8('api', '\uCC98\uB9AC \uC644\uB8CC', '201, \uC751\uB2F5\uC744 \uD0A4\uC640 \uD568\uAED8 \uC800\uC7A5', 'ok');
      r.s8(new Long(2000, 0));
      r.r8('client', '\uC7AC\uC2DC\uB3C4(\uC751\uB2F5\uC744 \uBABB \uBC1B\uC558\uB2E4\uACE0 \uAC00\uC815)', '\uAC19\uC740 \uD0A4, \uAC19\uC740 \uBCF8\uBB38 A', 'info');
      var d = Idempotency_instance.h6(rec, hashA, r.p8_1);
      r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '\uC800\uC7A5\uB41C \uC751\uB2F5\uC744 \uADF8\uB300\uB85C \uB3CC\uB824\uC900\uB2E4: ' + toString(d), 'ok', 'Idempotency.decide(COMPLETED, A, now)');
      ok = d instanceof Replay;
      verdict = '\uC8FC\uBB38 1\uAC74, \uC751\uB2F5 \uB3D9\uC77C(order 1001)';
      break;
    case 'in-flight':
      r.s8(new Long(40, 0));
      r.r8('client', '\uC7AC\uC694\uCCAD', '\uAC19\uC740 \uD0A4, \uAC19\uC740 \uBCF8\uBB38 A (\uCCAB \uC694\uCCAD \uCC98\uB9AC \uC911)', 'info');
      var d_0 = Idempotency_instance.h6(rec, hashA, r.p8_1);
      r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '409 Conflict: ' + toString(d_0), 'warn', 'Idempotency.decide(IN_PROGRESS, A, ' + r.p8_1.toString() + ')');
      ok = d_0 instanceof InProgress;
      r.s8(new Long(300, 0));
      rec = new IdemRecord(hashA, IdemStatus_COMPLETED_getInstance(), new Long(0, 0), 201, '{order:1001,PAID}');
      r.r8('api', '\uCCAB \uC694\uCCAD \uC644\uB8CC', '201', 'ok');
      var d2 = Idempotency_instance.h6(rec, hashA, r.p8_1);
      r.r8('client', '\uB2E4\uC2DC \uC7AC\uC2DC\uB3C4', toString(d2), 'ok', 'Idempotency.decide(COMPLETED, A, now)');
      var tmp_0;
      if (ok) {
        tmp_0 = d2 instanceof Replay;
      } else {
        tmp_0 = false;
      }

      ok = tmp_0;
      verdict = '\uCC98\uB9AC \uC911\uC5D0\uB294 409, \uC644\uB8CC \uB4A4\uC5D0\uB294 \uAC19\uC740 \uC751\uB2F5. \uC8FC\uBB38 1\uAC74';
      break;
    case 'mismatch':
      r.s8(new Long(150, 0));
      rec = new IdemRecord(hashA, IdemStatus_COMPLETED_getInstance(), new Long(0, 0), 201, '{order:1001,PAID}');
      r.r8('api', '\uCC98\uB9AC \uC644\uB8CC', '201', 'ok');
      r.s8(new Long(500, 0));
      r.r8('client', '\uAC19\uC740 \uD0A4, \uBCF8\uBB38 B(\uC218\uB7C9 \uBCC0\uACBD)', 'Idempotency-Key: k-2001', 'info');
      var d_1 = Idempotency_instance.h6(rec, 'sha:body-B', r.p8_1);
      r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '422 \uAC70\uBD80: ' + toString(d_1), 'error', 'Idempotency.decide(COMPLETED, B, now)');
      ok = d_1 instanceof Mismatch;
      verdict = '\uB2E4\uB978 \uBCF8\uBB38\uC740 \uAC70\uBD80, \uC8FC\uBB38\uC740 1\uAC74 \uADF8\uB300\uB85C';
      break;
    default:
      r.r8('api', '\uCC98\uB9AC \uC911 \uD504\uB85C\uC138\uC2A4 \uC885\uB8CC', '\uD0A4\uB294 IN_PROGRESS \uB85C \uB0A8\uB294\uB2E4(\uC7A0\uAE08 30\uCD08)', 'error');
      r.s8(new Long(31000, 0));
      r.r8('client', '\uC7AC\uC2DC\uB3C4', '\uAC19\uC740 \uD0A4, \uAC19\uC740 \uBCF8\uBB38 A', 'info');
      var d_2 = Idempotency_instance.h6(rec, hashA, r.p8_1);
      r.r8('api', '\uBA71\uB4F1 \uD310\uC815', '\uC7A0\uAE08\uC774 \uC9C0\uB098 \uC774\uC5B4\uBC1B\uB294\uB2E4: ' + toString(d_2), 'warn', 'Idempotency.decide(IN_PROGRESS, A, ' + r.p8_1.toString() + ') // lockedUntil=30000');
      r.r8('api', '\uC774\uC5B4\uBC1B\uAE30', '\uD0A4\uB85C \uC774\uBBF8 \uB9CC\uB4E0 \uC8FC\uBB38\uC744 \uCC3E\uC544 \uC0C8\uB85C \uB9CC\uB4E4\uC9C0 \uC54A\uACE0 \uB9C8\uBB34\uB9AC', 'ok');
      ok = d_2 instanceof Reclaim;
      verdict = '\uC0C8 \uC8FC\uBB38\uC744 \uB9CC\uB4E4\uC9C0 \uC54A\uACE0 \uAE30\uC874 \uC8FC\uBB38\uC744 \uB9C8\uBB34\uB9AC';
      break;
  }
  return new SimResult('duplicate-request', variant, title, r.o8_1, listOf_0(to('\uBA71\uB4F1 \uD0A4', 'k-2001')), ok, verdict);
}
function Buyer(id, qty) {
  qty = qty === VOID ? 1 : qty;
  this.w8_1 = id;
  this.x8_1 = qty;
}
protoOf(Buyer).toString = function () {
  return 'Buyer(id=' + this.w8_1 + ', qty=' + this.x8_1 + ')';
};
protoOf(Buyer).hashCode = function () {
  var result = this.w8_1;
  result = imul(result, 31) + this.x8_1 | 0;
  return result;
};
protoOf(Buyer).equals = function (other) {
  if (this === other)
    return true;
  if (!(other instanceof Buyer))
    return false;
  if (!(this.w8_1 === other.w8_1))
    return false;
  if (!(this.x8_1 === other.x8_1))
    return false;
  return true;
};
function concurrentOrder($this, variant, stockQty, buyerCount) {
  var r = new Recorder();
  // Inline function 'kotlin.collections.map' call
  var this_0 = numberRangeToNumber(1, buyerCount);
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
  var inductionVariable = this_0.x4_1;
  var last = this_0.y4_1;
  if (inductionVariable <= last)
    do {
      var item = inductionVariable;
      inductionVariable = inductionVariable + 1 | 0;
      var it = item;
      var tmp$ret$2 = new Buyer(it);
      destination.h1(tmp$ret$2);
    }
     while (!(item === last));
  var buyers = destination;
  var cell = new StockCell(stockQty);
  // Inline function 'kotlin.collections.mutableListOf' call
  var sold = ArrayList_init_$Create$_0();
  // Inline function 'kotlin.collections.mutableListOf' call
  var soldOut = ArrayList_init_$Create$_0();
  r.r8('setup', '\uCD08\uAE30 \uC7AC\uACE0', '\uC7AC\uACE0 ' + stockQty + ', \uB3D9\uC2DC\uC5D0 \uC8FC\uBB38\uD558\uB294 \uC0AC\uB78C ' + buyers.k() + '\uBA85', 'info');
  switch (variant) {
    case 'naive':
      // Inline function 'kotlin.collections.associate' call

      var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(buyers, 10)), 16);
      // Inline function 'kotlin.collections.associateTo' call

      var destination_0 = LinkedHashMap_init_$Create$(capacity);
      var _iterator__ex2g4s = buyers.e();
      while (_iterator__ex2g4s.f()) {
        var element = _iterator__ex2g4s.g();
        r.s8(new Long(1, 0));
        r.r8('buyer#' + element.w8_1, '\uC7AC\uACE0 \uC77D\uAE30', 'available=' + cell.i6_1, 'info', 'SELECT available');
        // Inline function 'kotlin.collections.plusAssign' call
        var pair = to(element.w8_1, cell);
        destination_0.w1(pair.q5_1, pair.r5_1);
      }

      var seen = destination_0;
      var _iterator__ex2g4s_0 = buyers.e();
      while (_iterator__ex2g4s_0.f()) {
        var b = _iterator__ex2g4s_0.g();
        var c = getValue(seen, b.w8_1);
        r.s8(new Long(1, 0));
        if (StockRules_instance.k6(c, b.x8_1)) {
          var tmp = c.i6_1 - b.x8_1 | 0;
          // Inline function 'kotlin.Long.plus' call
          var this_1 = cell.j6_1;
          var tmp$ret$9 = add(this_1, fromInt(1));
          cell = new StockCell(tmp, tmp$ret$9);
          // Inline function 'kotlin.collections.plusAssign' call
          var element_0 = b.w8_1;
          sold.h1(element_0);
          r.q8('buyer#' + b.w8_1, '\uB36E\uC5B4\uC4F0\uAE30', '\uC77D\uC740 ' + c.i6_1 + ' \uAE30\uC900\uC73C\uB85C ' + cell.i6_1 + ' \uAE30\uB85D \u2192 \uC608\uC57D\uB428', 'warn', 'UPDATE stock SET available = ' + cell.i6_1, 'available=' + cell.i6_1);
        } else {
          // Inline function 'kotlin.collections.plusAssign' call
          var element_1 = b.w8_1;
          soldOut.h1(element_1);
          r.r8('buyer#' + b.w8_1, '\uD488\uC808', '\uC77D\uC740 \uAC12\uC774 0', 'info');
        }
      }

      break;
    case 'optimistic':
      var pending = buyers;
      var round = 1;
      $l$loop: while (true) {
        // Inline function 'kotlin.collections.isNotEmpty' call
        if (!!pending.l()) {
          break $l$loop;
        }
        // Inline function 'kotlin.collections.associate' call
        var this_2 = pending;
        var capacity_0 = coerceAtLeast(mapCapacity(collectionSizeOrDefault(this_2, 10)), 16);
        // Inline function 'kotlin.collections.associateTo' call
        var destination_1 = LinkedHashMap_init_$Create$(capacity_0);
        var _iterator__ex2g4s_1 = this_2.e();
        while (_iterator__ex2g4s_1.f()) {
          var element_2 = _iterator__ex2g4s_1.g();
          // Inline function 'kotlin.collections.plusAssign' call
          var pair_0 = to(element_2.w8_1, cell);
          destination_1.w1(pair_0.q5_1, pair_0.r5_1);
        }
        var reads = destination_1;
        // Inline function 'kotlin.collections.mutableListOf' call
        var retry = ArrayList_init_$Create$_0();
        var _iterator__ex2g4s_2 = pending.e();
        $l$loop_0: while (_iterator__ex2g4s_2.f()) {
          var b_0 = _iterator__ex2g4s_2.g();
          r.s8(new Long(1, 0));
          var seen_0 = getValue(reads, b_0.w8_1);
          if (!StockRules_instance.k6(seen_0, b_0.x8_1)) {
            // Inline function 'kotlin.collections.plusAssign' call
            var element_3 = b_0.w8_1;
            soldOut.h1(element_3);
            r.r8('buyer#' + b_0.w8_1, '\uD488\uC808', '\uC77D\uC740 \uC7AC\uACE0 ' + seen_0.i6_1 + ', \uB77C\uC6B4\uB4DC ' + round, 'info', 'StockRules.canReserve(StockCell(' + seen_0.i6_1 + '), ' + b_0.x8_1 + ') = false');
            continue $l$loop_0;
          }
          if (equalsLong(cell.j6_1, seen_0.j6_1)) {
            cell = StockRules_instance.l6(cell, b_0.x8_1);
            // Inline function 'kotlin.collections.plusAssign' call
            var element_4 = b_0.w8_1;
            sold.h1(element_4);
            r.q8('buyer#' + b_0.w8_1, '\uBC84\uC804 \uC77C\uCE58: \uC608\uC57D', 'v' + seen_0.j6_1.toString() + ' \u2192 v' + cell.j6_1.toString() + ', \uC7AC\uACE0 ' + cell.i6_1, 'ok', 'UPDATE ... WHERE version=' + seen_0.j6_1.toString() + ' AND available>=' + b_0.x8_1, 'available=' + cell.i6_1);
          } else {
            // Inline function 'kotlin.collections.plusAssign' call
            retry.h1(b_0);
            r.r8('buyer#' + b_0.w8_1, '\uBC84\uC804 \uBD88\uC77C\uCE58: \uC7AC\uC2DC\uB3C4', '\uC77D\uC740 v' + seen_0.j6_1.toString() + ', \uD604\uC7AC v' + cell.j6_1.toString(), 'warn', 'UPDATE ... WHERE version=' + seen_0.j6_1.toString() + ' \u2192 0 rows');
          }
        }
        pending = retry;
        round = round + 1 | 0;
      }

      break;
    default:
      var _iterator__ex2g4s_3 = buyers.e();
      while (_iterator__ex2g4s_3.f()) {
        var b_1 = _iterator__ex2g4s_3.g();
        r.s8(new Long(1, 0));
        r.r8('buyer#' + b_1.w8_1, '\uD589 \uC7A0\uAE08 \uD68D\uB4DD', 'SELECT ... FOR UPDATE (\uC55E \uC0AC\uB78C\uC774 \uB05D\uB0A0 \uB54C\uAE4C\uC9C0 \uB300\uAE30)', 'info');
        if (StockRules_instance.k6(cell, b_1.x8_1)) {
          cell = StockRules_instance.l6(cell, b_1.x8_1);
          // Inline function 'kotlin.collections.plusAssign' call
          var element_5 = b_1.w8_1;
          sold.h1(element_5);
          r.q8('buyer#' + b_1.w8_1, '\uC608\uC57D', '\uC7AC\uACE0 ' + cell.i6_1, 'ok', 'StockRules.reserve', 'available=' + cell.i6_1);
        } else {
          // Inline function 'kotlin.collections.plusAssign' call
          var element_6 = b_1.w8_1;
          soldOut.h1(element_6);
          r.r8('buyer#' + b_1.w8_1, '\uD488\uC808', '\uC7AC\uACE0 ' + cell.i6_1, 'info', 'StockRules.canReserve = false');
        }
        r.r8('buyer#' + b_1.w8_1, '\uC7A0\uAE08 \uD574\uC81C', '\uCEE4\uBC0B', 'info');
      }

      break;
  }
  var oversold = coerceAtLeast(sold.k() - stockQty | 0, 0);
  var ok = oversold === 0;
  var verdict = ok ? '\uCD08\uACFC \uD310\uB9E4 \uC5C6\uC74C: ' + sold.k() + '\uAC74 \uD310\uB9E4, ' + soldOut.k() + '\uAC74 \uD488\uC808' : '\uCD08\uACFC \uD310\uB9E4 ' + oversold + '\uAC74: \uC7AC\uACE0 ' + stockQty + '\uAC1C\uC5D0 ' + sold.k() + '\uAC74 \uD310\uB9E4';
  var title;
  switch (variant) {
    case 'naive':
      title = '\uC7A0\uAE08 \uC5C6\uC774 \uC77D\uACE0 \uB36E\uC5B4\uC4F0\uAE30(\uBE44\uAD50\uC6A9)';
      break;
    case 'optimistic':
      title = '\uB099\uAD00\uC801 \uC7A0\uAE08: \uBC84\uC804 \uBE44\uAD50 \uD6C4 \uC7AC\uC2DC\uB3C4';
      break;
    default:
      title = '\uBE44\uAD00\uC801 \uC7A0\uAE08: \uD589 \uC7A0\uAE08 \uD6C4 \uCC98\uB9AC';
      break;
  }
  r.r8('result', '\uC815\uB9AC', verdict, ok ? 'ok' : 'error');
  return new SimResult('concurrent-order', variant, title, r.o8_1, listOf([to('\uCD08\uAE30 \uC7AC\uACE0', '' + stockQty), to('\uB3D9\uC2DC \uC8FC\uBB38', '' + buyers.k()), to('\uD310\uB9E4', '' + sold.k()), to('\uD488\uC808 \uC751\uB2F5', '' + soldOut.k()), to('\uCD08\uACFC \uD310\uB9E4', '' + oversold)]), ok, verdict);
}
function paymentTimeout($this, variant) {
  var r = new Recorder();
  var policy = new PaymentPolicy(3);
  var s = {_v: PaymentMachine_instance.p7(new Money(new Long(10000, 0)))};
  r.q8('api', '\uC2B9\uC778 \uC694\uCCAD', '10000\uC6D0, \uC751\uB2F5 \uB300\uAE30 1.5\uCD08', 'info', 'PaymentMachine.initial(10000)', s._v.y6());
  r.s8(new Long(1500, 0));
  r.r8('gateway', '\uC751\uB2F5 \uC5C6\uC74C', '\uC2DC\uAC04 \uCD08\uACFC: \uC2B9\uC778\uB410\uB294\uC9C0 \uBAA8\uB978\uB2E4', 'warn');
  paymentTimeout$step(s, policy, r, Outcome_TIMEOUT_getInstance(), Source_REQUEST_getInstance(), '\uACB0\uACFC \uBD88\uBA85', 'warn');
  var stockReleased = false;
  switch (variant) {
    case 'approved-by-inquiry':
      r.s8(new Long(1000, 0));
      r.r8('job', '\uC7AC\uC870\uD68C #1', '\uAC8C\uC774\uD2B8\uC6E8\uC774: \uCC98\uB9AC \uC911', 'info');
      paymentTimeout$step(s, policy, r, Outcome_PENDING_getInstance(), Source_INQUIRY_getInstance(), '\uC544\uC9C1 \uCC98\uB9AC \uC911');
      r.s8(new Long(1000, 0));
      r.r8('job', '\uC7AC\uC870\uD68C #2', '\uAC8C\uC774\uD2B8\uC6E8\uC774: \uC2B9\uC778', 'ok');
      paymentTimeout$step(s, policy, r, Outcome_APPROVED_getInstance(), Source_INQUIRY_getInstance(), '\uC870\uD68C\uB85C \uD655\uC815', 'ok');
      break;
    case 'declined-not-found':
      r.s8(new Long(1000, 0));
      r.r8('job', '\uC7AC\uC870\uD68C #1', '\uAC8C\uC774\uD2B8\uC6E8\uC774: \uD574\uB2F9 \uAC70\uB798 \uC5C6\uC74C(\uC694\uCCAD\uC774 \uB3C4\uCC29\uD558\uC9C0 \uC54A\uC558\uB2E4)', 'info');
      paymentTimeout$step(s, policy, r, Outcome_NOT_FOUND_getInstance(), Source_INQUIRY_getInstance(), '\uC5C6\uC74C \u2192 \uC2B9\uC778\uB41C \uC801 \uC5C6\uB2E4\uACE0 \uD655\uC815', 'warn');
      stockReleased = true;
      r.r8('inventory', '\uC7AC\uACE0 \uBC18\uD658', '\uC608\uC57D\uD588\uB358 \uC218\uB7C9\uC744 \uB3CC\uB824\uB193\uB294\uB2E4', 'ok', 'StockRules.release');
      break;
    default:
      var inductionVariable = 1;
      if (inductionVariable <= 3)
        do {
          var i = inductionVariable;
          inductionVariable = inductionVariable + 1 | 0;
          r.s8(new Long(1000, 0));
          r.r8('job', '\uC7AC\uC870\uD68C #' + i, '\uAC8C\uC774\uD2B8\uC6E8\uC774: \uCC98\uB9AC \uC911', 'info');
          paymentTimeout$step(s, policy, r, Outcome_PENDING_getInstance(), Source_INQUIRY_getInstance(), '\uC544\uC9C1 \uCC98\uB9AC \uC911');
        }
         while (inductionVariable <= 3);
      r.r8('payment', '\uBCF4\uB958', '\uD69F\uC218\uB97C \uB118\uACA8 HELD \uB85C \uB450\uACE0 \uC8FC\uBB38\uC740 \uACB0\uC81C \uB300\uAE30\uB85C \uC751\uB2F5', 'warn', VOID, s._v.y6());
      r.s8(new Long(10000, 0));
      r.r8('job', '\uBCF4\uB958 \uC7AC\uC870\uD68C', '\uAC8C\uC774\uD2B8\uC6E8\uC774: \uC2B9\uC778', 'ok');
      paymentTimeout$step(s, policy, r, Outcome_APPROVED_getInstance(), Source_INQUIRY_getInstance(), '\uBCF4\uB958 \uAC74 \uD655\uC815', 'ok');
      break;
  }
  var ok = s._v.z6();
  var verdict = '\uACB0\uC81C ' + s._v.y6() + (stockReleased ? ', \uC7AC\uACE0 \uBC18\uD658' : '');
  var tmp;
  var tmp_0 = s._v;
  if (tmp_0 instanceof Approved) {
    tmp = 'PAID';
  } else {
    tmp = 'PAYMENT_FAILED';
  }
  r.r8('order', '\uC8FC\uBB38 \uC0C1\uD0DC', tmp, ok ? 'ok' : 'error');
  return new SimResult('payment-timeout', variant, '\uACB0\uC81C \uC2B9\uC778 \uC751\uB2F5\uC774 \uC2DC\uAC04 \uCD08\uACFC: \uACB0\uACFC \uBD88\uBA85\uC744 \uC870\uD68C\uB85C \uD655\uC815', r.o8_1, listOf_0(to('\uCD5C\uC885', s._v.y6())), ok, verdict);
}
function callbacks($this) {
  var r = new Recorder();
  var s = {_v: PaymentMachine_instance.p7(new Money(new Long(10000, 0)))};
  s._v = PaymentMachine_instance.r7(s._v, Outcome_TIMEOUT_getInstance(), Source_REQUEST_getInstance()).j7();
  r.q8('payment', '\uC2DC\uAC04 \uCD08\uACFC', '\uACB0\uACFC \uBD88\uBA85\uC73C\uB85C \uC2DC\uC791: ' + s._v.y6(), 'warn', 'PaymentMachine.on(REQUESTED, TIMEOUT, REQUEST)', s._v.y6());
  var applied = {_v: 0};
  var ignored = {_v: 0};
  var rejected = {_v: 0};
  callbacks$cb(r, s, applied, ignored, rejected, 2, Outcome_APPROVED_getInstance(), '\uC21C\uC11C\uAC00 \uB4A4\uBC14\uB00C\uC5B4 \uC2B9\uC778\uC774 \uBA3C\uC800 \uB3C4\uCC29');
  callbacks$cb(r, s, applied, ignored, rejected, 2, Outcome_APPROVED_getInstance(), '\uAC19\uC740 \uCF5C\uBC31 \uC7AC\uC804\uC1A1');
  callbacks$cb(r, s, applied, ignored, rejected, 1, Outcome_PENDING_getInstance(), '\uCC98\uB9AC \uC911 \uD1B5\uC9C0\uAC00 \uB4A4\uB2A6\uAC8C \uB3C4\uCC29');
  callbacks$cb(r, s, applied, ignored, rejected, 3, Outcome_DECLINED_getInstance(), '\uC774\uBBF8 \uC2B9\uC778\uB41C \uACB0\uC81C\uC5D0 \uAC70\uC808 \uD1B5\uC9C0');
  var tmp;
  var tmp_0;
  var tmp_1 = s._v;
  if (tmp_1 instanceof Approved) {
    tmp_0 = applied._v === 1;
  } else {
    tmp_0 = false;
  }
  if (tmp_0) {
    tmp = rejected._v === 1;
  } else {
    tmp = false;
  }
  var ok = tmp;
  return new SimResult('callbacks', 'default', '\uC911\uBCF5\xB7\uC21C\uC11C \uB4A4\uBC14\uB010 \uCF5C\uBC31', r.o8_1, listOf([to('\uBC18\uC601', '' + applied._v), to('\uBB34\uC2DC', '' + ignored._v), to('\uAC70\uBD80', '' + rejected._v), to('\uCD5C\uC885', s._v.y6())]), ok, '\uACB0\uC81C ' + s._v.y6() + ': \uBC18\uC601 ' + applied._v + ', \uBB34\uC2DC ' + ignored._v + ', \uAC70\uBD80(\uBD88\uC77C\uCE58 \uAE30\uB85D) ' + rejected._v);
}
function consumerFailure($this, variant) {
  var r = new Recorder();
  var policy = new RetryPolicy(3, new Long(200, 0));
  var failures = variant === 'recovers' ? 2 : 99;
  r.r8('relay', '\uBC1C\uD589', 'PAYMENT_APPROVED(event e-77)\uB97C \uBE0C\uB85C\uCEE4\uB85C', 'info');
  var _iterator__ex2g4s = listOf(['kitchen', 'ledger']).e();
  while (_iterator__ex2g4s.f()) {
    var c = _iterator__ex2g4s.g();
    r.s8(new Long(5, 0));
    r.r8('consumer:' + c, '\uC18C\uBE44', '\uCC98\uB9AC \uC644\uB8CC', 'ok', 'Consumption.onArrival(false) = ' + toString(Consumption_instance.y5(false)));
  }
  var attempt = 1;
  var deadLettered = false;
  var processed = false;
  $l$loop_0: while (true) {
    r.s8(new Long(20, 0));
    if (attempt > failures) {
      processed = true;
      r.r8('consumer:notification', '\uC2DC\uB3C4 #' + attempt, '\uCC98\uB9AC \uC644\uB8CC', 'ok');
      break $l$loop_0;
    }
    r.r8('consumer:notification', '\uC2DC\uB3C4 #' + attempt, '\uC54C\uB9BC \uC804\uC1A1 \uC2E4\uD328', 'error');
    var d = Consumption_instance.z5(attempt, policy);
    if (d instanceof Retry) {
      r.r8('consumer:notification', '\uC7AC\uC2DC\uB3C4 \uC608\uC57D', d.x5_1.toString() + 'ms \uB4A4 #' + d.w5_1, 'warn', 'Consumption.onFailure(' + attempt + ', ' + policy.u5_1 + '\uD68C) = Retry');
      r.s8(d.x5_1);
      attempt = d.w5_1;
    } else {
      deadLettered = true;
      r.r8('consumer:notification', 'dead-letter', '' + attempt + ' \uD68C \uC2E4\uD328, \uC774\uBCA4\uD2B8\uB97C \uBCF4\uAD00\uD558\uACE0 \uC18C\uBE44\uB97C \uC774\uC5B4\uAC04\uB2E4', 'error', 'Consumption.onFailure(' + attempt + ', ' + policy.u5_1 + '\uD68C) = ' + toString(d));
      break $l$loop_0;
    }
  }
  var replayOk = true;
  if (deadLettered) {
    r.s8(new Long(5000, 0));
    r.r8('ops', 'dead-letter \uC7AC\uCC98\uB9AC \uBA85\uB839', '\uC6D0\uC778\uC744 \uACE0\uCE5C \uB4A4 event e-77 \uC744 \uB2E4\uC2DC \uBC1C\uD589', 'info');
    var _iterator__ex2g4s_0 = listOf(['kitchen', 'ledger']).e();
    while (_iterator__ex2g4s_0.f()) {
      var c_0 = _iterator__ex2g4s_0.g();
      var d_0 = Consumption_instance.y5(true);
      var tmp;
      if (replayOk) {
        tmp = d_0 instanceof SkipDuplicate;
      } else {
        tmp = false;
      }
      replayOk = tmp;
      r.r8('consumer:' + c_0, '\uC7AC\uC804\uB2EC \uC218\uC2E0', '\uC774\uBBF8 \uCC98\uB9AC\uD55C \uC774\uBCA4\uD2B8 ID \uB77C \uAC74\uB108\uB700: ' + toString(d_0), 'info', 'Consumption.onArrival(true)');
    }
    r.r8('consumer:notification', '\uC7AC\uC804\uB2EC \uC218\uC2E0', '\uCC98\uC74C \uCC98\uB9AC \u2192 \uC644\uB8CC', 'ok', 'Consumption.onArrival(false)');
    processed = true;
  }
  var ok = processed && replayOk;
  return new SimResult('consumer-failure', variant, variant === 'recovers' ? '\uC18C\uBE44\uC790\uAC00 \uC77C\uC2DC \uC2E4\uD328 \uD6C4 \uC7AC\uC2DC\uB3C4\uB85C \uD68C\uBCF5' : '\uC18C\uBE44\uC790 \uC2E4\uD328\uAC00 \uACC4\uC18D\uB418\uC5B4 dead-letter \uB85C \uAC04 \uB4A4 \uC7AC\uCC98\uB9AC', r.o8_1, listOf([to('\uC7AC\uC2DC\uB3C4 \uC815\uCC45', '\uCD5C\uB300 ' + policy.u5_1 + '\uD68C, 200ms \uBD80\uD130 2\uBC30'), to('dead-letter', deadLettered ? '\uBCF4\uAD00 \uD6C4 \uC7AC\uCC98\uB9AC' : '\uC5C6\uC74C')]), ok, deadLettered ? 'dead-letter \uBCF4\uAD00 \u2192 \uC7AC\uCC98\uB9AC\uB85C \uC54C\uB9BC 1\uAC74 \uCC98\uB9AC, \uB2E4\uB978 \uC18C\uBE44\uC790\uB294 \uC911\uBCF5 \uAC74\uB108\uB700' : '\uC7AC\uC2DC\uB3C4 ' + attempt + '\uBC88\uC9F8\uC5D0 \uCC98\uB9AC, \uC54C\uB9BC 1\uAC74');
}
function paymentTimeout$step(s, policy, r, o, src, note, tone) {
  tone = tone === VOID ? 'info' : tone;
  var before = s._v.y6();
  var t = PaymentMachine_instance.q7(s._v, o, src, policy);
  s._v = t.j7();
  var tmp;
  if (t instanceof Applied) {
    tmp = '\uC804\uC774';
  } else {
    if (t instanceof Ignored) {
      tmp = '\uBB34\uC2DC(' + t.l7_1 + ')';
    } else {
      if (t instanceof Rejected) {
        tmp = '\uAC70\uBD80(' + t.n7_1 + ')';
      } else {
        noWhenBranchMatchedException();
      }
    }
  }
  var kind = tmp;
  r.q8('payment', kind, note + ': ' + before + ' \u2192 ' + s._v.y6(), tone, 'PaymentMachine.on(' + before + ', ' + o.toString() + ', ' + src.toString() + ')', s._v.y6());
}
function callbacks$cb(r, s, applied, ignored, rejected, seq, o, note) {
  r.s8(new Long(30, 0));
  var before = s._v.y6();
  var t = PaymentMachine_instance.r7(s._v, o, Source_CALLBACK_getInstance());
  s._v = t.j7();
  if (t instanceof Applied) {
    applied._v = applied._v + 1 | 0;
    r.q8('callback', '\uCF5C\uBC31 #' + seq + ' ' + o.toString(), note + ': ' + before + ' \u2192 ' + s._v.y6(), 'ok', 'PaymentMachine.on(' + before + ', ' + o.toString() + ', CALLBACK)', s._v.y6());
  } else {
    if (t instanceof Ignored) {
      ignored._v = ignored._v + 1 | 0;
      r.q8('callback', '\uCF5C\uBC31 #' + seq + ' ' + o.toString(), note + ': \uBB34\uC2DC(' + t.l7_1 + ')', 'info', 'PaymentMachine.on(' + before + ', ' + o.toString() + ', CALLBACK)', s._v.y6());
    } else {
      if (t instanceof Rejected) {
        rejected._v = rejected._v + 1 | 0;
        r.q8('callback', '\uCF5C\uBC31 #' + seq + ' ' + o.toString(), note + ': \uAC70\uBD80(' + t.n7_1 + ') \u2014 \uBD88\uC77C\uCE58\uB85C \uAE30\uB85D', 'error', 'PaymentMachine.on(' + before + ', ' + o.toString() + ', CALLBACK)', s._v.y6());
      } else {
        noWhenBranchMatchedException();
      }
    }
  }
}
function Simulation() {
  Simulation_instance = this;
  this.u8_1 = listOf(['happy', 'duplicate-request', 'concurrent-order', 'payment-timeout', 'callbacks', 'consumer-failure']);
  this.v8_1 = listOf_0(new OrderLine(new Long(1, 0), 2, new Money(new Long(4500, 0)), new Money(new Long(500, 0))));
}
protoOf(Simulation).y8 = function (scenario) {
  switch (scenario) {
    case 'duplicate-request':
      return listOf(['replay', 'in-flight', 'mismatch', 'reclaim']);
    case 'concurrent-order':
      return listOf(['optimistic', 'pessimistic', 'naive']);
    case 'payment-timeout':
      return listOf(['approved-by-inquiry', 'declined-not-found', 'held-then-resolved']);
    case 'consumer-failure':
      return listOf(['recovers', 'dead-letter']);
    default:
      return listOf_0('default');
  }
};
protoOf(Simulation).z8 = function (scenario, variant, params) {
  var tmp;
  switch (scenario) {
    case 'happy':
      tmp = happy(this);
      break;
    case 'duplicate-request':
      tmp = duplicateRequest(this, variant);
      break;
    case 'concurrent-order':
      var tmp1_safe_receiver = params.s('stock');
      var tmp2_elvis_lhs = tmp1_safe_receiver == null ? null : toIntOrNull(tmp1_safe_receiver);
      var tmp_0 = tmp2_elvis_lhs == null ? 1 : tmp2_elvis_lhs;
      var tmp3_safe_receiver = params.s('buyers');
      var tmp4_elvis_lhs = tmp3_safe_receiver == null ? null : toIntOrNull(tmp3_safe_receiver);
      tmp = concurrentOrder(this, variant, tmp_0, tmp4_elvis_lhs == null ? 5 : tmp4_elvis_lhs);
      break;
    case 'payment-timeout':
      tmp = paymentTimeout(this, variant);
      break;
    case 'callbacks':
      tmp = callbacks(this);
      break;
    case 'consumer-failure':
      tmp = consumerFailure(this, variant);
      break;
    default:
      throw IllegalArgumentException_init_$Create$('\uC54C \uC218 \uC5C6\uB294 \uC2DC\uB098\uB9AC\uC624: ' + scenario);
  }
  return tmp;
};
var Simulation_instance;
function Simulation_getInstance() {
  if (Simulation_instance == null)
    new Simulation();
  return Simulation_instance;
}
function runScenario(scenario, variant, params) {
  // Inline function 'kotlin.collections.filter' call
  var tmp0 = split(params, charArrayOf([_Char___init__impl__6a9atx(38)]));
  // Inline function 'kotlin.collections.filterTo' call
  var destination = ArrayList_init_$Create$_0();
  var _iterator__ex2g4s = tmp0.e();
  while (_iterator__ex2g4s.f()) {
    var element = _iterator__ex2g4s.g();
    if (contains(element, _Char___init__impl__6a9atx(61))) {
      destination.h1(element);
    }
  }
  // Inline function 'kotlin.collections.associate' call
  var capacity = coerceAtLeast(mapCapacity(collectionSizeOrDefault(destination, 10)), 16);
  // Inline function 'kotlin.collections.associateTo' call
  var destination_0 = LinkedHashMap_init_$Create$(capacity);
  var _iterator__ex2g4s_0 = destination.e();
  while (_iterator__ex2g4s_0.f()) {
    var element_0 = _iterator__ex2g4s_0.g();
    // Inline function 'kotlin.collections.plusAssign' call
    var pair = to(substringBefore(element_0, _Char___init__impl__6a9atx(61)), substringAfter(element_0, _Char___init__impl__6a9atx(61)));
    destination_0.w1(pair.q5_1, pair.r5_1);
  }
  var map = destination_0;
  return Simulation_getInstance().z8(scenario, variant, map).n8();
}
function listScenarios() {
  var tmp = Json_instance;
  // Inline function 'kotlin.collections.map' call
  var this_0 = Simulation_getInstance().u8_1;
  // Inline function 'kotlin.collections.mapTo' call
  var destination = ArrayList_init_$Create$(collectionSizeOrDefault(this_0, 10));
  var _iterator__ex2g4s = this_0.e();
  while (_iterator__ex2g4s.f()) {
    var item = _iterator__ex2g4s.g();
    var tmp$ret$2 = new RawJson(Json_instance.o6([to('id', item), to('variants', Simulation_getInstance().y8(item))]));
    destination.h1(tmp$ret$2);
  }
  return tmp.o6([to('scenarios', destination)]);
}
//region block: post-declaration
protoOf(Requested).z6 = get_resolved;
protoOf(Unknown).z6 = get_resolved;
protoOf(Held).z6 = get_resolved;
protoOf(Declined).z6 = get_resolved;
protoOf(Approved).z6 = get_resolved;
//endregion
//region block: init
Process_instance = new Process();
SkipDuplicate_instance = new SkipDuplicate();
DeadLetter_instance = new DeadLetter();
Consumption_instance = new Consumption();
Proceed_instance = new Proceed();
Mismatch_instance = new Mismatch();
InProgress_instance = new InProgress();
Reclaim_instance = new Reclaim();
Idempotency_instance = new Idempotency();
StockRules_instance = new StockRules();
Json_instance = new Json();
Math2_instance = new Math2();
PaymentMachine_instance = new PaymentMachine();
Companion_instance_0 = new Companion_0();
Pricing_instance = new Pricing();
//endregion
//region block: exports
export {
  runScenario as runScenario,
  listScenarios as listScenarios,
};
//endregion

//# sourceMappingURL=orderlane-domain.mjs.map
