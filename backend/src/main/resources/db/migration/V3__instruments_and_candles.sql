-- =====================================================================
-- EXCHANGE PRO - V3: instrument master data and OHLCV candles
--
-- IMPORTANT: reference_price, annual_volatility and avg_daily_volume are
-- PARAMETERS FOR THE PRICE SIMULATOR (rough orders of magnitude so the
-- simulation looks plausible). They are NOT quotes, are not kept up to
-- date, and must never be displayed as real market prices.
-- Company names and NSE symbols are public reference information.
-- =====================================================================

CREATE TABLE instruments (
    id                UUID          PRIMARY KEY,
    symbol            VARCHAR(20)   NOT NULL,
    name              VARCHAR(120)  NOT NULL,
    exchange          VARCHAR(8)    NOT NULL,
    sector            VARCHAR(60)   NOT NULL,
    currency          VARCHAR(3)    NOT NULL DEFAULT 'INR',
    tick_size         NUMERIC(10,4) NOT NULL,
    lot_size          INTEGER       NOT NULL,
    reference_price   NUMERIC(19,4) NOT NULL,   -- simulator seed only
    annual_volatility NUMERIC(6,4)  NOT NULL,   -- simulator parameter (0.25 = 25 % per year)
    avg_daily_volume  BIGINT        NOT NULL,   -- simulator parameter (shares)
    active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version           BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uq_instruments_symbol      UNIQUE (symbol),
    CONSTRAINT ck_instruments_symbol      CHECK (symbol ~ '^[A-Z0-9&-]{1,20}$'),
    CONSTRAINT ck_instruments_exchange    CHECK (exchange IN ('NSE')),
    CONSTRAINT ck_instruments_tick        CHECK (tick_size > 0),
    CONSTRAINT ck_instruments_lot         CHECK (lot_size > 0),
    CONSTRAINT ck_instruments_ref_price   CHECK (reference_price > 0),
    CONSTRAINT ck_instruments_volatility  CHECK (annual_volatility > 0 AND annual_volatility < 2),
    CONSTRAINT ck_instruments_volume      CHECK (avg_daily_volume > 0)
);

CREATE INDEX ix_instruments_sector ON instruments (sector);
-- Case-insensitive prefix/substring search on symbol and name.
CREATE INDEX ix_instruments_name_lower ON instruments (lower(name));

-- OHLCV candles. Two stored resolutions:
--   '1m' : one-minute candles (5m/15m/1h are aggregated from these at query time)
--   '1d' : daily candles, day boundary = midnight Asia/Kolkata (IST)
-- source records where the candle came from; it is always SIMULATED today.
CREATE TABLE candles (
    instrument_id UUID          NOT NULL REFERENCES instruments (id),
    timeframe     VARCHAR(3)    NOT NULL,
    open_time     TIMESTAMPTZ   NOT NULL,
    open          NUMERIC(19,4) NOT NULL,
    high          NUMERIC(19,4) NOT NULL,
    low           NUMERIC(19,4) NOT NULL,
    close         NUMERIC(19,4) NOT NULL,
    volume        BIGINT        NOT NULL,
    source        VARCHAR(16)   NOT NULL DEFAULT 'SIMULATED',
    PRIMARY KEY (instrument_id, timeframe, open_time),
    CONSTRAINT ck_candles_timeframe CHECK (timeframe IN ('1m', '1d')),
    CONSTRAINT ck_candles_ohlc      CHECK (low > 0 AND low <= open AND low <= close AND high >= open AND high >= close),
    CONSTRAINT ck_candles_volume    CHECK (volume >= 0),
    CONSTRAINT ck_candles_source    CHECK (source IN ('SIMULATED', 'REAL'))
);

-- Seed: large NSE-listed companies. (symbol, name, sector, sim reference price, sim volatility, sim avg volume)
INSERT INTO instruments (id, symbol, name, exchange, sector, tick_size, lot_size, reference_price, annual_volatility, avg_daily_volume)
SELECT gen_random_uuid(), s.symbol, s.name, 'NSE', s.sector, 0.05, 1, s.ref_price, s.vol, s.adv
FROM (VALUES
    ('RELIANCE',   'Reliance Industries Ltd',                 'Oil & Gas',              1400.00, 0.22, 10000000),
    ('TCS',        'Tata Consultancy Services Ltd',           'Information Technology', 3100.00, 0.21,  2500000),
    ('HDFCBANK',   'HDFC Bank Ltd',                           'Financial Services',      980.00, 0.20, 15000000),
    ('INFY',       'Infosys Ltd',                             'Information Technology', 1500.00, 0.24,  7000000),
    ('ICICIBANK',  'ICICI Bank Ltd',                          'Financial Services',     1400.00, 0.22, 12000000),
    ('HINDUNILVR', 'Hindustan Unilever Ltd',                  'FMCG',                   2450.00, 0.18,  1500000),
    ('ITC',        'ITC Ltd',                                 'FMCG',                    410.00, 0.18, 14000000),
    ('SBIN',       'State Bank of India',                     'Financial Services',      870.00, 0.26, 12000000),
    ('BHARTIARTL', 'Bharti Airtel Ltd',                       'Telecommunication',      2000.00, 0.22,  6000000),
    ('KOTAKBANK',  'Kotak Mahindra Bank Ltd',                 'Financial Services',     2100.00, 0.22,  3500000),
    ('LT',         'Larsen & Toubro Ltd',                     'Construction',           3900.00, 0.24,  2000000),
    ('AXISBANK',   'Axis Bank Ltd',                           'Financial Services',     1200.00, 0.25,  7000000),
    ('ASIANPAINT', 'Asian Paints Ltd',                        'Consumer Durables',      2500.00, 0.22,  1200000),
    ('MARUTI',     'Maruti Suzuki India Ltd',                 'Automobile',            16000.00, 0.22,   400000),
    ('SUNPHARMA',  'Sun Pharmaceutical Industries Ltd',       'Healthcare',             1700.00, 0.22,  2500000),
    ('TITAN',      'Titan Company Ltd',                       'Consumer Durables',      3700.00, 0.24,  1000000),
    ('BAJFINANCE', 'Bajaj Finance Ltd',                       'Financial Services',     1000.00, 0.30,  6000000),
    ('ULTRACEMCO', 'UltraTech Cement Ltd',                    'Construction Materials',12000.00, 0.22,   300000),
    ('WIPRO',      'Wipro Ltd',                               'Information Technology',  245.00, 0.26, 10000000),
    ('NESTLEIND',  'Nestle India Ltd',                        'FMCG',                   1250.00, 0.18,  1000000),
    ('HCLTECH',    'HCL Technologies Ltd',                    'Information Technology', 1550.00, 0.24,  3000000),
    ('TATASTEEL',  'Tata Steel Ltd',                          'Metals & Mining',         170.00, 0.32, 30000000),
    ('POWERGRID',  'Power Grid Corporation of India Ltd',     'Power',                   290.00, 0.24, 12000000),
    ('NTPC',       'NTPC Ltd',                                'Power',                   340.00, 0.26, 12000000),
    ('ONGC',       'Oil and Natural Gas Corporation Ltd',     'Oil & Gas',               245.00, 0.30, 12000000),
    ('M&M',        'Mahindra & Mahindra Ltd',                 'Automobile',             3600.00, 0.28,  2500000),
    ('ADANIENT',   'Adani Enterprises Ltd',                   'Metals & Mining',        2400.00, 0.40,  2000000),
    ('ADANIPORTS', 'Adani Ports and Special Economic Zone Ltd','Services',              1450.00, 0.34,  3000000),
    ('JSWSTEEL',   'JSW Steel Ltd',                           'Metals & Mining',        1150.00, 0.30,  2500000),
    ('COALINDIA',  'Coal India Ltd',                          'Metals & Mining',         385.00, 0.28,  8000000),
    ('TECHM',      'Tech Mahindra Ltd',                       'Information Technology', 1500.00, 0.28,  2500000),
    ('DRREDDY',    'Dr. Reddy''s Laboratories Ltd',           'Healthcare',             1250.00, 0.22,  2000000),
    ('CIPLA',      'Cipla Ltd',                               'Healthcare',             1520.00, 0.22,  2000000),
    ('GRASIM',     'Grasim Industries Ltd',                   'Construction Materials', 2800.00, 0.24,   800000),
    ('HINDALCO',   'Hindalco Industries Ltd',                 'Metals & Mining',         800.00, 0.32,  7000000),
    ('BAJAJFINSV', 'Bajaj Finserv Ltd',                       'Financial Services',     2050.00, 0.26,  1200000),
    ('BRITANNIA',  'Britannia Industries Ltd',                'FMCG',                   5900.00, 0.20,   300000),
    ('EICHERMOT',  'Eicher Motors Ltd',                       'Automobile',             7000.00, 0.26,   500000),
    ('HEROMOTOCO', 'Hero MotoCorp Ltd',                       'Automobile',             5500.00, 0.26,   600000),
    ('DIVISLAB',   'Divi''s Laboratories Ltd',                'Healthcare',             6500.00, 0.26,   400000),
    ('APOLLOHOSP', 'Apollo Hospitals Enterprise Ltd',         'Healthcare',             7600.00, 0.24,   400000),
    ('INDUSINDBK', 'IndusInd Bank Ltd',                       'Financial Services',      750.00, 0.36,  5000000),
    ('TATACONSUM', 'Tata Consumer Products Ltd',              'FMCG',                   1150.00, 0.22,  1500000)
) AS s(symbol, name, sector, ref_price, vol, adv);
