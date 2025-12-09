package systems.project.cache;

import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.Statistics;
import org.springframework.stereotype.Component;
import systems.project.configuratons.CacheLoggingProperties;

import java.util.logging.Logger;

@Aspect
@Component
public class CacheStatisticsAspect {

    private final CacheLoggingProperties properties;
    private final EntityManagerFactory entityManagerFactory;
    private final Logger logger;

    public CacheStatisticsAspect(CacheLoggingProperties properties,
                                 EntityManagerFactory entityManagerFactory,
                                 Logger logger) {
        this.properties = properties;
        this.entityManagerFactory = entityManagerFactory;
        this.logger = logger;
    }

    @Around("@annotation(systems.project.cache.CacheStatsTracked)")
    public Object logCacheStats(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!properties.isLoggingEnabled()) {
            return joinPoint.proceed();
        }

        Snapshot before = snapshot();
        try {
            Object result = joinPoint.proceed();
            logDelta(joinPoint, before);
            return result;
        } catch (Throwable throwable) {
            logDelta(joinPoint, before);
            throw throwable;
        }
    }

    private Snapshot snapshot() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        Statistics statistics = sessionFactory.getStatistics();
        return new Snapshot(statistics.getSecondLevelCacheHitCount(),
                statistics.getSecondLevelCacheMissCount(),
                statistics.getSecondLevelCachePutCount());
    }

    private void logDelta(ProceedingJoinPoint joinPoint, Snapshot before) {
        Snapshot after = snapshot();
        long hitDelta = after.hits - before.hits;
        long missDelta = after.misses - before.misses;
        long putDelta = after.puts - before.puts;
        if (hitDelta == 0 && missDelta == 0 && putDelta == 0) {
            return;
        }
        logger.info(String.format("L2 cache [%s] hits %+d, misses %+d, puts %+d (totals H=%d/M=%d/P=%d)",
                joinPoint.getSignature().toShortString(),
                hitDelta, missDelta, putDelta,
                after.hits, after.misses, after.puts));
    }

    private record Snapshot(long hits, long misses, long puts) { }
}
