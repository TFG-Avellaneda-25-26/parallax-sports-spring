-- Crea la tabla usada por la entidad PandaScoreMatch
-- Ejecuta este archivo con psql, pgAdmin, DBeaver o tu cliente de PostgreSQL preferido.

CREATE TABLE IF NOT EXISTS public.pandascore_matches (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pandascore_id BIGINT NOT NULL,
    name VARCHAR(1024) NOT NULL,
    slug VARCHAR(512),
    league_name VARCHAR(255),
    videogame VARCHAR(128),
    status VARCHAR(50),
    begin_at TIMESTAMP WITH TIME ZONE,
    end_at TIMESTAMP WITH TIME ZONE,
    raw_json text,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Índices útiles
CREATE INDEX IF NOT EXISTS idx_pandascore_matches_pandascore_id ON public.pandascore_matches(pandascore_id);
CREATE INDEX IF NOT EXISTS idx_pandascore_matches_league_name ON public.pandascore_matches(league_name);
CREATE INDEX IF NOT EXISTS idx_pandascore_matches_begin_at ON public.pandascore_matches(begin_at);


-- Restricción de unicidad en pandascore_id: crear sólo si no existe (compatible con todas las versiones)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_pandascore_matches_pandascore_id'
    ) THEN
        ALTER TABLE public.pandascore_matches
            ADD CONSTRAINT uq_pandascore_matches_pandascore_id UNIQUE (pandascore_id);
    END IF;
END$$;

-- Ejemplo de inserción de prueba (descomenta si quieres probar manualmente):
-- INSERT INTO public.pandascore_matches (pandascore_id, name, slug, league_name, status, begin_at)
-- VALUES (123456, 'Test Match', 'test-match', 'league-of-legends', 'not_started', now());

-- Comprobaciones útiles:
-- SELECT count(*) FROM public.pandascore_matches;
-- SELECT * FROM public.pandascore_matches ORDER BY begin_at DESC LIMIT 10;


