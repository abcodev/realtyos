"""
단지 기본정보 xlsx → DB 적재 스크립트

Usage:
    python3 scripts/seed_apartment_complex.py [xlsx_path]

환경변수 (기본값은 로컬 개발 DB):
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
"""

import os
import sys
import warnings
import psycopg2
import psycopg2.extras
import openpyxl

DB_HOST     = os.environ.get("DB_HOST", "127.0.0.1")
DB_PORT     = os.environ.get("DB_PORT", "15432")
DB_NAME     = os.environ.get("DB_NAME", "realtyos")
DB_USER     = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "postgres")

DEFAULT_XLSX = os.path.join(os.path.dirname(__file__), "data", "20260630_단지_기본정보.xlsx")

# xlsx 컬럼 인덱스 (row 2가 헤더, row 3부터 데이터)
COL = {
    "시도": 0, "시군구": 1, "읍면": 2, "동리": 3,
    "단지코드": 4, "단지명": 5, "단지분류": 6, "법정동주소": 7,
    "우편번호": 8, "도로명주소": 9, "분양형태": 10,
    "사용승인일": 11, "동수": 13, "세대수": 14,
    "관리방식": 19, "난방방식": 20, "복도유형": 21,
    "시공사": 22, "시행사": 23,
    "관리사무소_연락처": 63, "관리사무소_팩스": 64,
    "최고층수": 66, "지하층수": 68,
    "전기차충전_지상": 82, "전기차충전_지하": 83,
}

BATCH_SIZE = 500


def s(val, max_len=None):
    """값을 문자열로 변환. 빈 문자열 → None."""
    if val is None:
        return None
    v = str(val).strip()
    if not v:
        return None
    return v[:max_len] if max_len and len(v) > max_len else v


def to_int(val):
    if val is None:
        return None
    try:
        return int(float(str(val).replace(",", "")))
    except (ValueError, TypeError):
        return None


def load_rows(xlsx_path):
    warnings.filterwarnings("ignore")
    wb = openpyxl.load_workbook(xlsx_path, data_only=True)
    ws = wb.active
    rows = []
    for row in ws.iter_rows(min_row=3, values_only=True):
        kapt_code = s(row[COL["단지코드"]])
        if not kapt_code:
            continue
        rows.append(row)
    wb.close()
    return rows


def upsert_complexes(cur, rows):
    sql = """
        INSERT INTO real_estate_apartment_complex
            (kapt_code, kapt_name, as1, as2, as3, as4, full_address, active)
        VALUES %s
        ON CONFLICT (kapt_code) DO UPDATE SET
            kapt_name    = EXCLUDED.kapt_name,
            as1          = EXCLUDED.as1,
            as2          = EXCLUDED.as2,
            as3          = EXCLUDED.as3,
            as4          = EXCLUDED.as4,
            full_address = EXCLUDED.full_address,
            active       = TRUE,
            updated_at   = CURRENT_TIMESTAMP
    """
    data = [
        (
            s(row[COL["단지코드"]], 30),
            s(row[COL["단지명"]], 255),
            s(row[COL["시도"]], 100),
            s(row[COL["시군구"]], 100),
            s(row[COL["읍면"]], 100),
            s(row[COL["동리"]], 100),
            s(row[COL["법정동주소"]], 500),
            True,
        )
        for row in rows
    ]
    psycopg2.extras.execute_values(cur, sql, data, page_size=BATCH_SIZE)
    return len(data)


def upsert_basis_info(cur, rows):
    sql = """
        INSERT INTO real_estate_apartment_complex_basis_info
            (kapt_code, kapt_name, zipcode, kapt_addr, doro_juso,
             code_apt_nm, code_sale_nm, kapt_usedate,
             kapt_dong_cnt, kaptda_cnt, ho_cnt,
             code_mgr_nm, code_heat_nm, code_hall_nm,
             kapt_bcompany, kapt_acompany,
             kapt_tel, kapt_fax,
             kapt_top_floor, kapt_base_floor, kaptd_ecntp,
             active)
        VALUES %s
        ON CONFLICT (kapt_code) DO UPDATE SET
            kapt_name      = EXCLUDED.kapt_name,
            zipcode        = EXCLUDED.zipcode,
            kapt_addr      = EXCLUDED.kapt_addr,
            doro_juso      = EXCLUDED.doro_juso,
            code_apt_nm    = EXCLUDED.code_apt_nm,
            code_sale_nm   = EXCLUDED.code_sale_nm,
            kapt_usedate   = EXCLUDED.kapt_usedate,
            kapt_dong_cnt  = EXCLUDED.kapt_dong_cnt,
            kaptda_cnt     = EXCLUDED.kaptda_cnt,
            ho_cnt         = EXCLUDED.ho_cnt,
            code_mgr_nm    = EXCLUDED.code_mgr_nm,
            code_heat_nm   = EXCLUDED.code_heat_nm,
            code_hall_nm   = EXCLUDED.code_hall_nm,
            kapt_bcompany  = EXCLUDED.kapt_bcompany,
            kapt_acompany  = EXCLUDED.kapt_acompany,
            kapt_tel       = EXCLUDED.kapt_tel,
            kapt_fax       = EXCLUDED.kapt_fax,
            kapt_top_floor = EXCLUDED.kapt_top_floor,
            kapt_base_floor= EXCLUDED.kapt_base_floor,
            kaptd_ecntp    = EXCLUDED.kaptd_ecntp,
            active         = TRUE,
            updated_at     = CURRENT_TIMESTAMP
    """

    def ev_count(row):
        a = to_int(row[COL["전기차충전_지상"]])
        b = to_int(row[COL["전기차충전_지하"]])
        if a is None and b is None:
            return None
        return (a or 0) + (b or 0)

    data = [
        (
            s(row[COL["단지코드"]], 30),
            s(row[COL["단지명"]], 255),
            s(row[COL["우편번호"]], 255),
            s(row[COL["법정동주소"]], 500),
            s(row[COL["도로명주소"]], 500),
            s(row[COL["단지분류"]], 255),
            s(row[COL["분양형태"]], 255),
            s(row[COL["사용승인일"]], 255),
            to_int(row[COL["동수"]]),
            s(row[COL["세대수"]], 255),
            to_int(row[COL["세대수"]]),
            s(row[COL["관리방식"]], 255),
            s(row[COL["난방방식"]], 255),
            s(row[COL["복도유형"]], 255),
            s(row[COL["시공사"]], 255),
            s(row[COL["시행사"]], 255),
            s(row[COL["관리사무소_연락처"]], 255),
            s(row[COL["관리사무소_팩스"]], 255),
            to_int(row[COL["최고층수"]]),
            to_int(row[COL["지하층수"]]),
            ev_count(row),
            True,
        )
        for row in rows
    ]
    psycopg2.extras.execute_values(cur, sql, data, page_size=BATCH_SIZE)
    return len(data)


def main():
    xlsx_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_XLSX

    print(f"[1/3] 파일 로드: {xlsx_path}")
    rows = load_rows(xlsx_path)
    print(f"      → {len(rows):,}건")

    conn = psycopg2.connect(
        host=DB_HOST, port=DB_PORT, dbname=DB_NAME,
        user=DB_USER, password=DB_PASSWORD
    )
    try:
        with conn:
            with conn.cursor() as cur:
                print("[2/3] real_estate_apartment_complex upsert 중...")
                n1 = upsert_complexes(cur, rows)
                print(f"      → {n1:,}건 완료")

                print("[3/3] real_estate_apartment_complex_basis_info upsert 중...")
                n2 = upsert_basis_info(cur, rows)
                print(f"      → {n2:,}건 완료")

        print("\n완료!")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
