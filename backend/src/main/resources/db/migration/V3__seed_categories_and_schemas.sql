-- Category tree
INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
VALUES (NULL, 'clothing', 'Giyim', 'Clothing', FALSE),
       (NULL, 'vehicles', 'Araçlar', 'Vehicles', FALSE),
       (NULL, 'electronics', 'Elektronik', 'Electronics', FALSE),
       (NULL, 'home_decor', 'Ev & Dekorasyon', 'Home & Decor', FALSE),
       (NULL, 'accessories', 'Aksesuar', 'Accessories', FALSE);

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'tshirt', 'Tişört', 'T-shirt', TRUE FROM categories WHERE code = 'clothing';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'jeans', 'Kot Pantolon', 'Jeans', TRUE FROM categories WHERE code = 'clothing';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'car', 'Otomobil', 'Car', TRUE FROM categories WHERE code = 'vehicles';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'phone', 'Telefon', 'Phone', TRUE FROM categories WHERE code = 'electronics';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'tv', 'Televizyon', 'TV', TRUE FROM categories WHERE code = 'electronics';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'vase', 'Vazo', 'Vase', TRUE FROM categories WHERE code = 'home_decor';

INSERT INTO categories (parent_id, code, name_tr, name_en, leaf)
SELECT id, 'handbag', 'Çanta', 'Handbag', TRUE FROM categories WHERE code = 'accessories';

-- T-shirt schema v1
-- color is multi-select: the model lists every visible color, most dominant first.
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": true,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "black",      "label_tr": "Siyah",        "label_en": "Black" },
        { "value": "white",      "label_tr": "Beyaz",        "label_en": "White" },
        { "value": "gray",       "label_tr": "Gri",          "label_en": "Gray" },
        { "value": "red",        "label_tr": "Kırmızı",      "label_en": "Red" },
        { "value": "burgundy",   "label_tr": "Bordo",        "label_en": "Burgundy" },
        { "value": "pink",       "label_tr": "Pembe",        "label_en": "Pink" },
        { "value": "orange",     "label_tr": "Turuncu",      "label_en": "Orange" },
        { "value": "yellow",     "label_tr": "Sarı",         "label_en": "Yellow" },
        { "value": "mustard",    "label_tr": "Hardal",       "label_en": "Mustard" },
        { "value": "green",      "label_tr": "Yeşil",        "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil",   "label_en": "Dark Green" },
        { "value": "mint",       "label_tr": "Mint",         "label_en": "Mint" },
        { "value": "olive",      "label_tr": "Zeytin Yeşili", "label_en": "Olive" },
        { "value": "khaki",      "label_tr": "Haki",         "label_en": "Khaki" },
        { "value": "turquoise",  "label_tr": "Turkuaz",      "label_en": "Turquoise" },
        { "value": "light_blue", "label_tr": "Açık Mavi",    "label_en": "Light Blue" },
        { "value": "blue",       "label_tr": "Mavi",         "label_en": "Blue" },
        { "value": "navy",       "label_tr": "Lacivert",     "label_en": "Navy" },
        { "value": "purple",     "label_tr": "Mor",          "label_en": "Purple" },
        { "value": "lilac",      "label_tr": "Lila",         "label_en": "Lilac" },
        { "value": "brown",      "label_tr": "Kahverengi",   "label_en": "Brown" },
        { "value": "beige",      "label_tr": "Bej",          "label_en": "Beige" },
        { "value": "cream",      "label_tr": "Krem",         "label_en": "Cream" }
      ]
    },
    {
      "key": "pattern",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Desen",
      "label_en": "Pattern",
      "values": [
        { "value": "solid",     "label_tr": "Düz",        "label_en": "Solid" },
        { "value": "striped",   "label_tr": "Çizgili",    "label_en": "Striped" },
        { "value": "printed",   "label_tr": "Baskılı",    "label_en": "Printed" },
        { "value": "plaid",     "label_tr": "Ekose",      "label_en": "Plaid" },
        { "value": "polka_dot", "label_tr": "Puantiyeli", "label_en": "Polka Dot" },
        { "value": "color_block", "label_tr": "Renk Bloklu", "label_en": "Color Block" },
        { "value": "other",     "label_tr": "Diğer",      "label_en": "Other" }
      ]
    },
    {
      "key": "neckline",
      "type": "enum",
      "required": false,
      "multi": false,
      "label_tr": "Yaka",
      "label_en": "Neckline",
      "values": [
        { "value": "crew",       "label_tr": "Bisiklet Yaka", "label_en": "Crew Neck" },
        { "value": "v_neck",     "label_tr": "V Yaka",        "label_en": "V-Neck" },
        { "value": "polo",       "label_tr": "Polo Yaka",     "label_en": "Polo" },
        { "value": "turtleneck", "label_tr": "Balıkçı Yaka",  "label_en": "Turtleneck" },
        { "value": "other",      "label_tr": "Diğer",         "label_en": "Other" }
      ]
    },
    {
      "key": "sleeve_length",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Kol Boyu",
      "label_en": "Sleeve Length",
      "values": [
        { "value": "short",      "label_tr": "Kısa",   "label_en": "Short" },
        { "value": "long",       "label_tr": "Uzun",   "label_en": "Long" },
        { "value": "sleeveless", "label_tr": "Kolsuz", "label_en": "Sleeveless" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'tshirt';

-- Jeans schema v1
-- color is multi-select with the same clothing palette, most dominant first.
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": true,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "black",      "label_tr": "Siyah",        "label_en": "Black" },
        { "value": "white",      "label_tr": "Beyaz",        "label_en": "White" },
        { "value": "gray",       "label_tr": "Gri",          "label_en": "Gray" },
        { "value": "red",        "label_tr": "Kırmızı",      "label_en": "Red" },
        { "value": "burgundy",   "label_tr": "Bordo",        "label_en": "Burgundy" },
        { "value": "pink",       "label_tr": "Pembe",        "label_en": "Pink" },
        { "value": "orange",     "label_tr": "Turuncu",      "label_en": "Orange" },
        { "value": "yellow",     "label_tr": "Sarı",         "label_en": "Yellow" },
        { "value": "mustard",    "label_tr": "Hardal",       "label_en": "Mustard" },
        { "value": "green",      "label_tr": "Yeşil",        "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil",   "label_en": "Dark Green" },
        { "value": "mint",       "label_tr": "Mint",         "label_en": "Mint" },
        { "value": "olive",      "label_tr": "Zeytin Yeşili", "label_en": "Olive" },
        { "value": "khaki",      "label_tr": "Haki",         "label_en": "Khaki" },
        { "value": "turquoise",  "label_tr": "Turkuaz",      "label_en": "Turquoise" },
        { "value": "light_blue", "label_tr": "Açık Mavi",    "label_en": "Light Blue" },
        { "value": "blue",       "label_tr": "Mavi",         "label_en": "Blue" },
        { "value": "navy",       "label_tr": "Lacivert",     "label_en": "Navy" },
        { "value": "purple",     "label_tr": "Mor",          "label_en": "Purple" },
        { "value": "lilac",      "label_tr": "Lila",         "label_en": "Lilac" },
        { "value": "brown",      "label_tr": "Kahverengi",   "label_en": "Brown" },
        { "value": "beige",      "label_tr": "Bej",          "label_en": "Beige" },
        { "value": "cream",      "label_tr": "Krem",         "label_en": "Cream" }
      ]
    },
    {
      "key": "fit",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Kesim",
      "label_en": "Fit",
      "values": [
        { "value": "skinny",   "label_tr": "Skinny",   "label_en": "Skinny" },
        { "value": "slim",     "label_tr": "Slim",     "label_en": "Slim" },
        { "value": "straight", "label_tr": "Düz",      "label_en": "Straight" },
        { "value": "regular",  "label_tr": "Regular",  "label_en": "Regular" },
        { "value": "wide",     "label_tr": "Bol Paça", "label_en": "Wide Leg" },
        { "value": "baggy",    "label_tr": "Baggy",    "label_en": "Baggy" }
      ]
    },
    {
      "key": "distressed",
      "type": "boolean",
      "required": false,
      "label_tr": "Yırtık/Eskitilmiş",
      "label_en": "Distressed"
    }
  ]
}'::jsonb
FROM categories WHERE code = 'jeans';

-- Car schema v1
-- Cars are effectively single-colored; color stays single-select.
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "black",      "label_tr": "Siyah",     "label_en": "Black" },
        { "value": "white",      "label_tr": "Beyaz",     "label_en": "White" },
        { "value": "gray",       "label_tr": "Gri",       "label_en": "Gray" },
        { "value": "silver",     "label_tr": "Gümüş",     "label_en": "Silver" },
        { "value": "red",        "label_tr": "Kırmızı",   "label_en": "Red" },
        { "value": "burgundy",   "label_tr": "Bordo",     "label_en": "Burgundy" },
        { "value": "orange",     "label_tr": "Turuncu",   "label_en": "Orange" },
        { "value": "yellow",     "label_tr": "Sarı",      "label_en": "Yellow" },
        { "value": "green",      "label_tr": "Yeşil",     "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil", "label_en": "Dark Green" },
        { "value": "light_blue", "label_tr": "Açık Mavi", "label_en": "Light Blue" },
        { "value": "blue",       "label_tr": "Mavi",      "label_en": "Blue" },
        { "value": "navy",       "label_tr": "Lacivert",  "label_en": "Navy" },
        { "value": "turquoise",  "label_tr": "Turkuaz",   "label_en": "Turquoise" },
        { "value": "purple",     "label_tr": "Mor",       "label_en": "Purple" },
        { "value": "brown",      "label_tr": "Kahverengi", "label_en": "Brown" },
        { "value": "beige",      "label_tr": "Bej",       "label_en": "Beige" },
        { "value": "gold",       "label_tr": "Altın",     "label_en": "Gold" },
        { "value": "other",      "label_tr": "Diğer",     "label_en": "Other" }
      ]
    },
    {
      "key": "body_type",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Kasa Tipi",
      "label_en": "Body Type",
      "values": [
        { "value": "sedan",       "label_tr": "Sedan",     "label_en": "Sedan" },
        { "value": "hatchback",   "label_tr": "Hatchback", "label_en": "Hatchback" },
        { "value": "suv",         "label_tr": "SUV",       "label_en": "SUV" },
        { "value": "pickup",      "label_tr": "Pickup",    "label_en": "Pickup" },
        { "value": "coupe",       "label_tr": "Coupe",     "label_en": "Coupe" },
        { "value": "convertible", "label_tr": "Cabrio",    "label_en": "Convertible" },
        { "value": "van",         "label_tr": "Van",       "label_en": "Van" },
        { "value": "minivan",     "label_tr": "Minivan",   "label_en": "Minivan" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'car';

-- Phone schema v1
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "black",     "label_tr": "Siyah",      "label_en": "Black" },
        { "value": "white",     "label_tr": "Beyaz",      "label_en": "White" },
        { "value": "gray",      "label_tr": "Gri",        "label_en": "Gray" },
        { "value": "silver",    "label_tr": "Gümüş",      "label_en": "Silver" },
        { "value": "gold",      "label_tr": "Altın",      "label_en": "Gold" },
        { "value": "rose_gold", "label_tr": "Roze Altın", "label_en": "Rose Gold" },
        { "value": "blue",      "label_tr": "Mavi",       "label_en": "Blue" },
        { "value": "navy",      "label_tr": "Lacivert",   "label_en": "Navy" },
        { "value": "light_blue", "label_tr": "Açık Mavi", "label_en": "Light Blue" },
        { "value": "green",     "label_tr": "Yeşil",      "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil", "label_en": "Dark Green" },
        { "value": "mint",      "label_tr": "Mint",       "label_en": "Mint" },
        { "value": "purple",    "label_tr": "Mor",        "label_en": "Purple" },
        { "value": "lilac",     "label_tr": "Lila",       "label_en": "Lilac" },
        { "value": "red",       "label_tr": "Kırmızı",    "label_en": "Red" },
        { "value": "burgundy",  "label_tr": "Bordo",      "label_en": "Burgundy" },
        { "value": "pink",      "label_tr": "Pembe",      "label_en": "Pink" },
        { "value": "yellow",    "label_tr": "Sarı",       "label_en": "Yellow" },
        { "value": "orange",    "label_tr": "Turuncu",    "label_en": "Orange" },
        { "value": "cream",     "label_tr": "Krem",       "label_en": "Cream" },
        { "value": "other",     "label_tr": "Diğer",      "label_en": "Other" }
      ]
    },
    {
      "key": "form_factor",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Form Faktörü",
      "label_en": "Form Factor",
      "values": [
        { "value": "bar",      "label_tr": "Düz",              "label_en": "Bar" },
        { "value": "foldable", "label_tr": "Katlanabilir",     "label_en": "Foldable" },
        { "value": "flip",     "label_tr": "Kapaklı (Flip)",   "label_en": "Flip" }
      ]
    },
    {
      "key": "camera_count",
      "type": "enum",
      "required": false,
      "multi": false,
      "label_tr": "Arka Kamera Sayısı",
      "label_en": "Rear Camera Count",
      "values": [
        { "value": "one",       "label_tr": "1",          "label_en": "1" },
        { "value": "two",       "label_tr": "2",          "label_en": "2" },
        { "value": "three",     "label_tr": "3",          "label_en": "3" },
        { "value": "four_plus", "label_tr": "4 ve üzeri", "label_en": "4+" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'phone';

-- TV schema v1
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "screen_form",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Ekran Formu",
      "label_en": "Screen Form",
      "values": [
        { "value": "flat",   "label_tr": "Düz",   "label_en": "Flat" },
        { "value": "curved", "label_tr": "Kavisli", "label_en": "Curved" }
      ]
    },
    {
      "key": "frame_color",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Çerçeve Rengi",
      "label_en": "Frame Color",
      "values": [
        { "value": "black",  "label_tr": "Siyah",  "label_en": "Black" },
        { "value": "gray",   "label_tr": "Gri",    "label_en": "Gray" },
        { "value": "silver", "label_tr": "Gümüş",  "label_en": "Silver" },
        { "value": "white",  "label_tr": "Beyaz",  "label_en": "White" },
        { "value": "other",  "label_tr": "Diğer",  "label_en": "Other" }
      ]
    },
    {
      "key": "stand_type",
      "type": "enum",
      "required": false,
      "multi": false,
      "label_tr": "Ayak Tipi",
      "label_en": "Stand Type",
      "values": [
        { "value": "two_legs",     "label_tr": "Çift Ayak",     "label_en": "Two Legs" },
        { "value": "center_stand", "label_tr": "Orta Ayak",     "label_en": "Center Stand" },
        { "value": "wall_mounted", "label_tr": "Duvara Monte",  "label_en": "Wall Mounted" },
        { "value": "other",        "label_tr": "Diğer",         "label_en": "Other" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'tv';

-- Vase schema v1
-- color is multi-select, most dominant first; transparent covers clear glass.
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": true,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "transparent", "label_tr": "Şeffaf",       "label_en": "Transparent" },
        { "value": "black",      "label_tr": "Siyah",        "label_en": "Black" },
        { "value": "white",      "label_tr": "Beyaz",        "label_en": "White" },
        { "value": "gray",       "label_tr": "Gri",          "label_en": "Gray" },
        { "value": "silver",     "label_tr": "Gümüş",        "label_en": "Silver" },
        { "value": "gold",       "label_tr": "Altın",        "label_en": "Gold" },
        { "value": "red",        "label_tr": "Kırmızı",      "label_en": "Red" },
        { "value": "burgundy",   "label_tr": "Bordo",        "label_en": "Burgundy" },
        { "value": "pink",       "label_tr": "Pembe",        "label_en": "Pink" },
        { "value": "orange",     "label_tr": "Turuncu",      "label_en": "Orange" },
        { "value": "yellow",     "label_tr": "Sarı",         "label_en": "Yellow" },
        { "value": "mustard",    "label_tr": "Hardal",       "label_en": "Mustard" },
        { "value": "green",      "label_tr": "Yeşil",        "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil",   "label_en": "Dark Green" },
        { "value": "mint",       "label_tr": "Mint",         "label_en": "Mint" },
        { "value": "turquoise",  "label_tr": "Turkuaz",      "label_en": "Turquoise" },
        { "value": "light_blue", "label_tr": "Açık Mavi",    "label_en": "Light Blue" },
        { "value": "blue",       "label_tr": "Mavi",         "label_en": "Blue" },
        { "value": "navy",       "label_tr": "Lacivert",     "label_en": "Navy" },
        { "value": "purple",     "label_tr": "Mor",          "label_en": "Purple" },
        { "value": "lilac",      "label_tr": "Lila",         "label_en": "Lilac" },
        { "value": "brown",      "label_tr": "Kahverengi",   "label_en": "Brown" },
        { "value": "beige",      "label_tr": "Bej",          "label_en": "Beige" },
        { "value": "cream",      "label_tr": "Krem",         "label_en": "Cream" }
      ]
    },
    {
      "key": "material",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Malzeme",
      "label_en": "Material",
      "values": [
        { "value": "ceramic", "label_tr": "Seramik",  "label_en": "Ceramic" },
        { "value": "glass",   "label_tr": "Cam",      "label_en": "Glass" },
        { "value": "metal",   "label_tr": "Metal",    "label_en": "Metal" },
        { "value": "wood",    "label_tr": "Ahşap",    "label_en": "Wood" },
        { "value": "plastic", "label_tr": "Plastik",  "label_en": "Plastic" },
        { "value": "stone",   "label_tr": "Taş",      "label_en": "Stone" },
        { "value": "wicker",  "label_tr": "Hasır",    "label_en": "Wicker" },
        { "value": "other",   "label_tr": "Diğer",    "label_en": "Other" }
      ]
    },
    {
      "key": "shape",
      "type": "enum",
      "required": false,
      "multi": false,
      "label_tr": "Form",
      "label_en": "Shape",
      "values": [
        { "value": "cylindrical", "label_tr": "Silindirik",  "label_en": "Cylindrical" },
        { "value": "spherical",   "label_tr": "Küresel",     "label_en": "Spherical" },
        { "value": "conical",     "label_tr": "Konik",       "label_en": "Conical" },
        { "value": "bottle",      "label_tr": "Şişe Formu",  "label_en": "Bottle" },
        { "value": "rectangular", "label_tr": "Köşeli",      "label_en": "Rectangular" },
        { "value": "other",       "label_tr": "Diğer",       "label_en": "Other" }
      ]
    },
    {
      "key": "pattern",
      "type": "enum",
      "required": false,
      "multi": false,
      "label_tr": "Desen",
      "label_en": "Pattern",
      "values": [
        { "value": "solid",     "label_tr": "Düz",        "label_en": "Solid" },
        { "value": "floral",    "label_tr": "Çiçekli",    "label_en": "Floral" },
        { "value": "geometric", "label_tr": "Geometrik",  "label_en": "Geometric" },
        { "value": "striped",   "label_tr": "Çizgili",    "label_en": "Striped" },
        { "value": "textured",  "label_tr": "Dokulu",     "label_en": "Textured" },
        { "value": "other",     "label_tr": "Diğer",      "label_en": "Other" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'vase';

-- Handbag schema v1
-- color and strap_type are multi-select (a bag often has both a top handle and a shoulder strap).
INSERT INTO category_schemas (category_id, version, schema)
SELECT id, 1, '{
  "attributes": [
    {
      "key": "color",
      "type": "enum",
      "required": true,
      "multi": true,
      "label_tr": "Renk",
      "label_en": "Color",
      "values": [
        { "value": "black",      "label_tr": "Siyah",        "label_en": "Black" },
        { "value": "white",      "label_tr": "Beyaz",        "label_en": "White" },
        { "value": "gray",       "label_tr": "Gri",          "label_en": "Gray" },
        { "value": "silver",     "label_tr": "Gümüş",        "label_en": "Silver" },
        { "value": "gold",       "label_tr": "Altın",        "label_en": "Gold" },
        { "value": "red",        "label_tr": "Kırmızı",      "label_en": "Red" },
        { "value": "burgundy",   "label_tr": "Bordo",        "label_en": "Burgundy" },
        { "value": "pink",       "label_tr": "Pembe",        "label_en": "Pink" },
        { "value": "orange",     "label_tr": "Turuncu",      "label_en": "Orange" },
        { "value": "yellow",     "label_tr": "Sarı",         "label_en": "Yellow" },
        { "value": "mustard",    "label_tr": "Hardal",       "label_en": "Mustard" },
        { "value": "green",      "label_tr": "Yeşil",        "label_en": "Green" },
        { "value": "dark_green", "label_tr": "Koyu Yeşil",   "label_en": "Dark Green" },
        { "value": "mint",       "label_tr": "Mint",         "label_en": "Mint" },
        { "value": "olive",      "label_tr": "Zeytin Yeşili", "label_en": "Olive" },
        { "value": "khaki",      "label_tr": "Haki",         "label_en": "Khaki" },
        { "value": "turquoise",  "label_tr": "Turkuaz",      "label_en": "Turquoise" },
        { "value": "light_blue", "label_tr": "Açık Mavi",    "label_en": "Light Blue" },
        { "value": "blue",       "label_tr": "Mavi",         "label_en": "Blue" },
        { "value": "navy",       "label_tr": "Lacivert",     "label_en": "Navy" },
        { "value": "purple",     "label_tr": "Mor",          "label_en": "Purple" },
        { "value": "lilac",      "label_tr": "Lila",         "label_en": "Lilac" },
        { "value": "brown",      "label_tr": "Kahverengi",   "label_en": "Brown" },
        { "value": "beige",      "label_tr": "Bej",          "label_en": "Beige" },
        { "value": "cream",      "label_tr": "Krem",         "label_en": "Cream" }
      ]
    },
    {
      "key": "material",
      "type": "enum",
      "required": true,
      "multi": false,
      "label_tr": "Malzeme",
      "label_en": "Material",
      "values": [
        { "value": "leather",      "label_tr": "Deri Görünümlü", "label_en": "Leather Look" },
        { "value": "suede",        "label_tr": "Süet",           "label_en": "Suede" },
        { "value": "fabric",       "label_tr": "Kumaş",          "label_en": "Fabric" },
        { "value": "canvas",       "label_tr": "Kanvas",         "label_en": "Canvas" },
        { "value": "straw_wicker", "label_tr": "Hasır",          "label_en": "Straw/Wicker" },
        { "value": "plastic",      "label_tr": "Plastik",        "label_en": "Plastic" },
        { "value": "other",        "label_tr": "Diğer",          "label_en": "Other" }
      ]
    },
    {
      "key": "strap_type",
      "type": "enum",
      "required": true,
      "multi": true,
      "label_tr": "Askı Tipi",
      "label_en": "Strap Type",
      "values": [
        { "value": "top_handle", "label_tr": "El Askılı",     "label_en": "Top Handle" },
        { "value": "shoulder",   "label_tr": "Omuz Askılı",   "label_en": "Shoulder" },
        { "value": "crossbody",  "label_tr": "Çapraz Askılı", "label_en": "Crossbody" },
        { "value": "clutch",     "label_tr": "Askısız (El Çantası)", "label_en": "Clutch" },
        { "value": "other",      "label_tr": "Diğer",         "label_en": "Other" }
      ]
    }
  ]
}'::jsonb
FROM categories WHERE code = 'handbag';
